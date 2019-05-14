package cn.ubwifi.probe.bean;


import java.io.Serializable;

public class ProbeStatus implements Serializable {
	private static final long serialVersionUID = 1L;
	private String Ap_mac;
	private long Timestamp;
	private int Freq;
	private int Signal;
	private int Associated;
	private String Client_mac;
	private String Bssid;
	private String Organization_id;
	private String Group_id;
	private String Asset_group_id;
	private String Province;
	private String City;
	private String District;

	public String getAp_mac() {
		return Ap_mac;
	}

	public void setAp_mac(String ap_mac) {
		Ap_mac = ap_mac;
	}

	public long getTimestamp() {
		return Timestamp;
	}

	public void setTimestamp(long timestamp) {
		Timestamp = timestamp;
	}

	public int getFreq() {
		return Freq;
	}

	public void setFreq(int freq) {
		Freq = freq;
	}

	public int getSignal() {
		return Signal;
	}

	public void setSignal(int signal) {
		Signal = signal;
	}

	public int getAssociated() {
		return Associated;
	}

	public void setAssociated(int associated) {
		Associated = associated;
	}

	public String getClient_mac() {
		return Client_mac;
	}

	public void setClient_mac(String client_mac) {
		Client_mac = client_mac;
	}

	public String getBssid() {
		return Bssid;
	}

	public void setBssid(String bssid) {
		Bssid = bssid;
	}

	public String getOrganization_id() {
		return Organization_id;
	}

	public void setOrganization_id(String organization_id) {
		Organization_id = organization_id;
	}

	public String getGroup_id() {
		return Group_id;
	}

	public void setGroup_id(String group_id) {
		Group_id = group_id;
	}

	public String getAsset_group_id() {
		return Asset_group_id;
	}

	public void setAsset_group_id(String asset_group_id) {
		Asset_group_id = asset_group_id;
	}

	public String getProvince() {
		return Province;
	}

	public void setProvince(String province) {
		Province = province;
	}

	public String getCity() {
		return City;
	}

	public void setCity(String city) {
		City = city;
	}

	public String getDistrict() {
		return District;
	}

	public void setDistrict(String district) {
		District = district;
	}

	@Override
	public String toString() {
		return "ProbeStatus{" +
				"Ap_mac='" + Ap_mac + '\'' +
				", Timestamp=" + Timestamp +
				", Freq=" + Freq +
				", Signal=" + Signal +
				", Associated=" + Associated +
				", Client_mac='" + Client_mac + '\'' +
				", Bssid='" + Bssid + '\'' +
				", Organization_id='" + Organization_id + '\'' +
				", Group_id='" + Group_id + '\'' +
				", Asset_group_id='" + Asset_group_id + '\'' +
				", Province='" + Province + '\'' +
				", City='" + City + '\'' +
				", District='" + District + '\'' +
				'}';
	}
}
