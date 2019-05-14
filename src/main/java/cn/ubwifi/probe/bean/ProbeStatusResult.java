package cn.ubwifi.probe.bean;

import java.io.Serializable;

public class ProbeStatusResult implements Serializable {
	private static final long serialVersionUID = 1L;

	private long Timestamp;
	private String Client_mac;
	private String Asset_group_id;
	private long Orig_row;

	//null, campatible for old druid record.
	private String Ap_mac;

	public long getTimestamp() {
		return Timestamp;
	}

	public void setTimestamp(long timestamp) {
		Timestamp = timestamp;
	}

	public String getClient_mac() {
		return Client_mac;
	}

	public void setClient_mac(String client_mac) {
		Client_mac = client_mac;
	}

	public String getAsset_group_id() {
		return Asset_group_id;
	}

	public void setAsset_group_id(String asset_group_id) {
		Asset_group_id = asset_group_id;
	}

	public long getOrig_row() {
		return Orig_row;
	}

	public void setOrig_row(long orig_row) {
		Orig_row = orig_row;
	}

	public String getAp_mac() {
		return Ap_mac;
	}

	public void setAp_mac(String ap_mac) {
		Ap_mac = ap_mac;
	}

	@Override
	public String toString() {
		return "ProbeStatusResult{" +
				"Timestamp=" + Timestamp +
				", Client_mac='" + Client_mac + '\'' +
				", Asset_group_id='" + Asset_group_id + '\'' +
				", Orig_row=" + Orig_row +
				", Ap_mac='" + Ap_mac + '\'' +
				'}';
	}
}
