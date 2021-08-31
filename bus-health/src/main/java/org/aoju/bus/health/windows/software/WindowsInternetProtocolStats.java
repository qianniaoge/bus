/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org OSHI and other contributors.                 *
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
package org.aoju.bus.health.windows.software;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.ptr.IntByReference;
import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.builtin.software.AbstractInternetProtocolStats;
import org.aoju.bus.health.builtin.software.InternetProtocolStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internet Protocol Stats implementation
 *
 * @author Kimi Liu
 * @version 6.2.8
 * @since JDK 1.8+
 */
@ThreadSafe
public class WindowsInternetProtocolStats extends AbstractInternetProtocolStats {

    private static final IPHlpAPI IPHLP = IPHlpAPI.INSTANCE;

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    private static List<IPConnection> queryTCPv4Connections() {
        List<IPConnection> conns = new ArrayList<>();
        // Get size needed
        IntByReference sizePtr = new IntByReference();
        IPHLP.GetExtendedTcpTable(null, sizePtr, false, IPHlpAPI.AF_INET, IPHlpAPI.TCP_TABLE_CLASS.TCP_TABLE_OWNER_PID_ALL, 0);
        // Get buffer and populate table
        int size;
        Memory buf;
        do {
            size = sizePtr.getValue();
            buf = new Memory(size);
            IPHLP.GetExtendedTcpTable(buf, sizePtr, false, IPHlpAPI.AF_INET, IPHlpAPI.TCP_TABLE_CLASS.TCP_TABLE_OWNER_PID_ALL, 0);
            // In case size changes and buffer was too small, repeat
        } while (size < sizePtr.getValue());
        IPHlpAPI.MIB_TCPTABLE_OWNER_PID tcpTable = new IPHlpAPI.MIB_TCPTABLE_OWNER_PID(buf);
        for (int i = 0; i < tcpTable.dwNumEntries; i++) {
            IPHlpAPI.MIB_TCPROW_OWNER_PID row = tcpTable.table[i];
            conns.add(new IPConnection("tcp4", Builder.parseIntToIP(row.dwLocalAddr),
                    Builder.bigEndian16ToLittleEndian(row.dwLocalPort), Builder.parseIntToIP(row.dwRemoteAddr),
                    Builder.bigEndian16ToLittleEndian(row.dwRemotePort), stateLookup(row.dwState), 0, 0,
                    row.dwOwningPid));
        }
        return conns;
    }

    private static List<IPConnection> queryTCPv6Connections() {
        List<IPConnection> conns = new ArrayList<>();
        // Get size needed
        IntByReference sizePtr = new IntByReference();
        IPHLP.GetExtendedTcpTable(null, sizePtr, false, IPHlpAPI.AF_INET6, IPHlpAPI.TCP_TABLE_CLASS.TCP_TABLE_OWNER_PID_ALL, 0);
        // Get buffer and populate table
        int size;
        Memory buf;
        do {
            size = sizePtr.getValue();
            buf = new Memory(size);
            IPHLP.GetExtendedTcpTable(buf, sizePtr, false, IPHlpAPI.AF_INET6, IPHlpAPI.TCP_TABLE_CLASS.TCP_TABLE_OWNER_PID_ALL, 0);
            // In case size changes and buffer was too small, repeat
        } while (size < sizePtr.getValue());
        IPHlpAPI.MIB_TCP6TABLE_OWNER_PID tcpTable = new IPHlpAPI.MIB_TCP6TABLE_OWNER_PID(buf);
        for (int i = 0; i < tcpTable.dwNumEntries; i++) {
            IPHlpAPI.MIB_TCP6ROW_OWNER_PID row = tcpTable.table[i];
            conns.add(new IPConnection("tcp6", row.LocalAddr, Builder.bigEndian16ToLittleEndian(row.dwLocalPort),
                    row.RemoteAddr, Builder.bigEndian16ToLittleEndian(row.dwRemotePort), stateLookup(row.State), 0, 0,
                    row.dwOwningPid));
        }
        return conns;
    }

    private static List<IPConnection> queryUDPv4Connections() {
        List<IPConnection> conns = new ArrayList<>();
        // Get size needed
        IntByReference sizePtr = new IntByReference();
        IPHLP.GetExtendedUdpTable(null, sizePtr, false, IPHlpAPI.AF_INET, IPHlpAPI.UDP_TABLE_CLASS.UDP_TABLE_OWNER_PID, 0);
        // Get buffer and populate table
        int size;
        Memory buf;
        do {
            size = sizePtr.getValue();
            buf = new Memory(size);
            IPHLP.GetExtendedUdpTable(buf, sizePtr, false, IPHlpAPI.AF_INET, IPHlpAPI.UDP_TABLE_CLASS.UDP_TABLE_OWNER_PID, 0);
            // In case size changes and buffer was too small, repeat
        } while (size < sizePtr.getValue());
        IPHlpAPI.MIB_UDPTABLE_OWNER_PID udpTable = new IPHlpAPI.MIB_UDPTABLE_OWNER_PID(buf);
        for (int i = 0; i < udpTable.dwNumEntries; i++) {
            IPHlpAPI.MIB_UDPROW_OWNER_PID row = udpTable.table[i];
            conns.add(new IPConnection("udp4", Builder.parseIntToIP(row.dwLocalAddr),
                    Builder.bigEndian16ToLittleEndian(row.dwLocalPort), new byte[0], 0, TcpState.NONE, 0, 0,
                    row.dwOwningPid));
        }
        return conns;
    }

    private static List<IPConnection> queryUDPv6Connections() {
        List<IPConnection> conns = new ArrayList<>();
        // Get size needed
        IntByReference sizePtr = new IntByReference();
        IPHLP.GetExtendedUdpTable(null, sizePtr, false, IPHlpAPI.AF_INET6, IPHlpAPI.UDP_TABLE_CLASS.UDP_TABLE_OWNER_PID, 0);
        // Get buffer and populate table
        int size;
        Memory buf;
        do {
            size = sizePtr.getValue();
            buf = new Memory(size);
            IPHLP.GetExtendedUdpTable(buf, sizePtr, false, IPHlpAPI.AF_INET6, IPHlpAPI.UDP_TABLE_CLASS.UDP_TABLE_OWNER_PID, 0);
            // In case size changes and buffer was too small, repeat
        } while (size < sizePtr.getValue());
        IPHlpAPI.MIB_UDP6TABLE_OWNER_PID udpTable = new IPHlpAPI.MIB_UDP6TABLE_OWNER_PID(buf);
        for (int i = 0; i < udpTable.dwNumEntries; i++) {
            IPHlpAPI.MIB_UDP6ROW_OWNER_PID row = udpTable.table[i];
            conns.add(new IPConnection("udp6", row.ucLocalAddr, Builder.bigEndian16ToLittleEndian(row.dwLocalPort),
                    new byte[0], 0, TcpState.NONE, 0, 0, row.dwOwningPid));
        }
        return conns;
    }

    private static TcpState stateLookup(int state) {
        switch (state) {
            case 1:
            case 12:
                return InternetProtocolStats.TcpState.CLOSED;
            case 2:
                return InternetProtocolStats.TcpState.LISTEN;
            case 3:
                return InternetProtocolStats.TcpState.SYN_SENT;
            case 4:
                return InternetProtocolStats.TcpState.SYN_RECV;
            case 5:
                return InternetProtocolStats.TcpState.ESTABLISHED;
            case 6:
                return InternetProtocolStats.TcpState.FIN_WAIT_1;
            case 7:
                return InternetProtocolStats.TcpState.FIN_WAIT_2;
            case 8:
                return InternetProtocolStats.TcpState.CLOSE_WAIT;
            case 9:
                return InternetProtocolStats.TcpState.CLOSING;
            case 10:
                return InternetProtocolStats.TcpState.LAST_ACK;
            case 11:
                return InternetProtocolStats.TcpState.TIME_WAIT;
            default:
                return InternetProtocolStats.TcpState.UNKNOWN;
        }
    }

    @Override
    public TcpStats getTCPv4Stats() {
        IPHlpAPI.MIB_TCPSTATS stats = new IPHlpAPI.MIB_TCPSTATS();
        IPHLP.GetTcpStatisticsEx(stats, IPHlpAPI.AF_INET);
        return new TcpStats(stats.dwCurrEstab, stats.dwActiveOpens, stats.dwPassiveOpens, stats.dwAttemptFails,
                stats.dwEstabResets, stats.dwOutSegs, stats.dwInSegs, stats.dwRetransSegs, stats.dwInErrs,
                stats.dwOutRsts);
    }

    @Override
    public TcpStats getTCPv6Stats() {
        IPHlpAPI.MIB_TCPSTATS stats = new IPHlpAPI.MIB_TCPSTATS();
        IPHLP.GetTcpStatisticsEx(stats, IPHlpAPI.AF_INET6);
        return new TcpStats(stats.dwCurrEstab, stats.dwActiveOpens, stats.dwPassiveOpens, stats.dwAttemptFails,
                stats.dwEstabResets, stats.dwOutSegs, stats.dwInSegs, stats.dwRetransSegs, stats.dwInErrs,
                stats.dwOutRsts);
    }

    @Override
    public UdpStats getUDPv4Stats() {
        IPHlpAPI.MIB_UDPSTATS stats = new IPHlpAPI.MIB_UDPSTATS();
        IPHLP.GetUdpStatisticsEx(stats, IPHlpAPI.AF_INET);
        return new UdpStats(stats.dwOutDatagrams, stats.dwInDatagrams, stats.dwNoPorts, stats.dwInErrors);
    }

    @Override
    public UdpStats getUDPv6Stats() {
        IPHlpAPI.MIB_UDPSTATS stats = new IPHlpAPI.MIB_UDPSTATS();
        IPHLP.GetUdpStatisticsEx(stats, IPHlpAPI.AF_INET6);
        return new UdpStats(stats.dwOutDatagrams, stats.dwInDatagrams, stats.dwNoPorts, stats.dwInErrors);
    }

    @Override
    public List<IPConnection> getConnections() {
        if (IS_VISTA_OR_GREATER) {
            List<IPConnection> conns = new ArrayList<>();
            conns.addAll(queryTCPv4Connections());
            conns.addAll(queryTCPv6Connections());
            conns.addAll(queryUDPv4Connections());
            conns.addAll(queryUDPv6Connections());
            return conns;
        }
        return Collections.emptyList();
    }

}
