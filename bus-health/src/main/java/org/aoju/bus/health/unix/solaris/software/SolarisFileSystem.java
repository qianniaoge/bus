/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2022 aoju.org OSHI and other contributors.                 *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 *                                                                               *
 ********************************************************************************/
package org.aoju.bus.health.unix.solaris.software;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;
import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.RegEx;
import org.aoju.bus.core.lang.tuple.Pair;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.Executor;
import org.aoju.bus.health.Memoize;
import org.aoju.bus.health.builtin.software.AbstractFileSystem;
import org.aoju.bus.health.builtin.software.OSFileStore;
import org.aoju.bus.health.unix.solaris.KstatKit;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The Solaris File System contains {@link OSFileStore}s which
 * are a storage pool, device, partition, volume, concrete file system or other
 * implementation specific means of file storage. In Solaris, these are found in
 * the /proc/mount filesystem, excluding temporary and kernel mounts.
 *
 * @author Kimi Liu
 * @version 6.3.5
 * @since JDK 1.8+
 */
@ThreadSafe
public class SolarisFileSystem extends AbstractFileSystem {

    public static final String SOLARIS_FS_PATH_EXCLUDES = "bus.health.os.solaris.filesystem.path.excludes";
    public static final String SOLARIS_FS_PATH_INCLUDES = "bus.health.os.solaris.filesystem.path.includes";
    public static final String SOLARIS_FS_VOLUME_EXCLUDES = "bus.health.os.solaris.filesystem.volume.excludes";
    public static final String SOLARIS_FS_VOLUME_INCLUDES = "bus.health.os.solaris.filesystem.volume.includes";
    private static final Supplier<Pair<Long, Long>> FILE_DESC = Memoize
            .memoize(SolarisFileSystem::queryFileDescriptors, Memoize.defaultExpiration());
    private static final List<PathMatcher> FS_PATH_EXCLUDES = Builder
            .loadAndParseFileSystemConfig(SOLARIS_FS_PATH_EXCLUDES);
    private static final List<PathMatcher> FS_PATH_INCLUDES = Builder
            .loadAndParseFileSystemConfig(SOLARIS_FS_PATH_INCLUDES);
    private static final List<PathMatcher> FS_VOLUME_EXCLUDES = Builder
            .loadAndParseFileSystemConfig(SOLARIS_FS_VOLUME_EXCLUDES);
    private static final List<PathMatcher> FS_VOLUME_INCLUDES = Builder
            .loadAndParseFileSystemConfig(SOLARIS_FS_VOLUME_INCLUDES);

    // Called by SolarisOSFileStore
    static List<OSFileStore> getFileStoreMatching(String nameToMatch) {
        return getFileStoreMatching(nameToMatch, false);
    }

    private static List<OSFileStore> getFileStoreMatching(String nameToMatch, boolean localOnly) {
        List<OSFileStore> fsList = new ArrayList<>();

        // Get inode usage data
        Map<String, Long> inodeFreeMap = new HashMap<>();
        Map<String, Long> inodeTotalMap = new HashMap<>();
        String key = null;
        String total = null;
        String free = null;
        String command = "df -g" + (localOnly ? " -l" : "");
        for (String line : Executor.runNative(command)) {
            /*- Sample Output:
            /                  (/dev/md/dsk/d0    ):         8192 block size          1024 frag size
            41310292 total blocks   18193814 free blocks 17780712 available        2486848 total files
             2293351 free files     22282240 filesys id
                 ufs fstype       0x00000004 flag             255 filename length
            */
            if (line.startsWith("/")) {
                key = RegEx.SPACES.split(line)[0];
                total = null;
            } else if (line.contains("available") && line.contains("total files")) {
                total = Builder.getTextBetweenStrings(line, "available", "total files").trim();
            } else if (line.contains("free files")) {
                free = Builder.getTextBetweenStrings(line, "", "free files").trim();
                if (key != null && total != null) {
                    inodeFreeMap.put(key, Builder.parseLongOrDefault(free, 0L));
                    inodeTotalMap.put(key, Builder.parseLongOrDefault(total, 0L));
                    key = null;
                }
            }
        }

        // Get mount table
        for (String fs : Executor.runNative("cat /etc/mnttab")) { // NOSONAR squid:S135
            String[] split = RegEx.SPACES.split(fs);
            if (split.length < 5) {
                continue;
            }
            // 1st field is volume name
            // 2nd field is mount point
            // 3rd field is fs type
            // 4th field is options
            // other fields ignored
            String volume = split[0];
            String path = split[1];
            String type = split[2];
            String options = split[3];

            // Skip non-local drives if requested, and exclude pseudo file systems
            if ((localOnly && NETWORK_FS_TYPES.contains(type))
                    || !path.equals("/") && (PSEUDO_FS_TYPES.contains(type) || Builder.isFileStoreExcluded(path,
                    volume, FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES))) {
                continue;
            }

            String name = path.substring(path.lastIndexOf('/') + 1);
            // Special case for /, pull last element of volume instead
            if (name.isEmpty()) {
                name = volume.substring(volume.lastIndexOf('/') + 1);
            }

            if (nameToMatch != null && !nameToMatch.equals(name)) {
                continue;
            }
            File f = new File(path);
            long totalSpace = f.getTotalSpace();
            long usableSpace = f.getUsableSpace();
            long freeSpace = f.getFreeSpace();

            String description;
            if (volume.startsWith("/dev") || path.equals("/")) {
                description = "Local Disk";
            } else if (volume.equals("tmpfs")) {
                description = "Ram Disk";
            } else if (NETWORK_FS_TYPES.contains(type)) {
                description = "Network Disk";
            } else {
                description = "Mount Point";
            }

            fsList.add(new SolarisOSFileStore(name, volume, name, path, options, "", "", description, type, freeSpace,
                    usableSpace, totalSpace, inodeFreeMap.containsKey(path) ? inodeFreeMap.get(path) : 0L,
                    inodeTotalMap.containsKey(path) ? inodeTotalMap.get(path) : 0L));
        }
        return fsList;
    }

    private static Pair<Long, Long> queryFileDescriptors() {
        Object[] results = KstatKit.queryKstat2("kstat:/kmem_cache/kmem_default/file_cache", "buf_inuse", "buf_max");
        long inuse = results[0] == null ? 0L : (long) results[0];
        long max = results[1] == null ? 0L : (long) results[1];
        return Pair.of(inuse, max);
    }

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        return getFileStoreMatching(null, localOnly);
    }

    @Override
    public long getOpenFileDescriptors() {
        if (SolarisOperatingSystem.HAS_KSTAT2) {
            // Use Kstat2 implementation
            return FILE_DESC.get().getLeft();
        }
        try (KstatKit.KstatChain kc = KstatKit.openChain()) {
            Kstat ksp = kc.lookup(null, -1, "file_cache");
            // Set values
            if (ksp != null && kc.read(ksp)) {
                return KstatKit.dataLookupLong(ksp, "buf_inuse");
            }
        }
        return 0L;
    }

    @Override
    public long getMaxFileDescriptors() {
        if (SolarisOperatingSystem.HAS_KSTAT2) {
            // Use Kstat2 implementation
            return FILE_DESC.get().getRight();
        }
        try (KstatKit.KstatChain kc = KstatKit.openChain()) {
            Kstat ksp = kc.lookup(null, -1, "file_cache");
            // Set values
            if (ksp != null && kc.read(ksp)) {
                return KstatKit.dataLookupLong(ksp, "buf_max");
            }
        }
        return 0L;
    }

}
