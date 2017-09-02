package kr.co.future.sslvpn.core.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class IfStatsLog {
	private Date date;

	String name;
	long rxBytes;
	long rxPackets;
	long rxErrs;
	long rxDrop;
	long rxFifo;
	long rxFrame;
	long rxCompressed;
	long rxMulticast;

	long txBytes;
	long txPackets;
	long txErrs;
	long txDrop;
	long txFifo;
	long txColls;
	long txCarrier;
	long txCompressed;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getRxBytes() {
		return rxBytes;
	}

	public void setRxBytes(long rxBytes) {
		this.rxBytes = rxBytes;
	}

	public long getRxPackets() {
		return rxPackets;
	}

	public void setRxPackets(long rxPackets) {
		this.rxPackets = rxPackets;
	}

	public long getRxErrs() {
		return rxErrs;
	}

	public void setRxErrs(long rxErrs) {
		this.rxErrs = rxErrs;
	}

	public long getRxDrop() {
		return rxDrop;
	}

	public void setRxDrop(long rxDrop) {
		this.rxDrop = rxDrop;
	}

	public long getRxFifo() {
		return rxFifo;
	}

	public void setRxFifo(long rxFifo) {
		this.rxFifo = rxFifo;
	}

	public long getRxFrame() {
		return rxFrame;
	}

	public void setRxFrame(long rxFrame) {
		this.rxFrame = rxFrame;
	}

	public long getRxCompressed() {
		return rxCompressed;
	}

	public void setRxCompressed(long rxCompressed) {
		this.rxCompressed = rxCompressed;
	}

	public long getRxMulticast() {
		return rxMulticast;
	}

	public void setRxMulticast(long rxMulticast) {
		this.rxMulticast = rxMulticast;
	}

	public long getTxBytes() {
		return txBytes;
	}

	public void setTxBytes(long txBytes) {
		this.txBytes = txBytes;
	}

	public long getTxPackets() {
		return txPackets;
	}

	public void setTxPackets(long txPackets) {
		this.txPackets = txPackets;
	}

	public long getTxErrs() {
		return txErrs;
	}

	public void setTxErrs(long txErrs) {
		this.txErrs = txErrs;
	}

	public long getTxDrop() {
		return txDrop;
	}

	public void setTxDrop(long txDrop) {
		this.txDrop = txDrop;
	}

	public long getTxFifo() {
		return txFifo;
	}

	public void setTxFifo(long txFifo) {
		this.txFifo = txFifo;
	}

	public long getTxColls() {
		return txColls;
	}

	public void setTxColls(long txColls) {
		this.txColls = txColls;
	}

	public long getTxCarrier() {
		return txCarrier;
	}

	public void setTxCarrier(long txCarrier) {
		this.txCarrier = txCarrier;
	}

	public long getTxCompressed() {
		return txCompressed;
	}

	public void setTxCompressed(long txCompressed) {
		this.txCompressed = txCompressed;
	}

	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ifname", name);
		m.put("rx_bytes", rxBytes);
		m.put("rx_packets", rxPackets);
		m.put("rx_errs", rxErrs);
		m.put("rx_drop", rxDrop);
		m.put("rx_fifo", rxFifo);
		m.put("rx_frame", rxFrame);
		m.put("rx_compressed", rxCompressed);
		m.put("rx_multicast", rxMulticast);

		m.put("tx_bytes", txBytes);
		m.put("tx_packets", txPackets);
		m.put("tx_errs", txErrs);
		m.put("tx_drop", txDrop);
		m.put("tx_fifo", txFifo);
		m.put("tx_colls", txColls);
		m.put("tx_carrier", txCarrier);
		m.put("tx_compressed", txCompressed);

		return new Log("ifstats", date, m);
	}

}
