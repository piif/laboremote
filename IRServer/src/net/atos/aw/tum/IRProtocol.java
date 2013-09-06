package net.atos.aw.tum;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IRProtocol {
	private static Map<String, IRProtocol> protocols;

	String name;
	byte frequency;
	byte cycle;
	short period;

	Map<String, short[]> keys;

	/**
	 * load protocol list from json config
	 * @param config
	 * @throws JSONException
	 */
	public static void loadProtocols(JSONObject config)
			throws JSONException {
		protocols = new HashMap<String, IRProtocol>();

		@SuppressWarnings("unchecked")
		Iterator<String> iter = config.keys();
		while(iter.hasNext()) {
			String name = iter.next();
			JSONObject protoConfig = config.getJSONObject(name);
			IRProtocol proto = new IRProtocol(name, protoConfig);
			protocols.put(name, proto);
		}
	}

	/**
	 * retrieve one of the protocols by its name
	 * @param name
	 * @return IRProtocol
	 */
	public static IRProtocol getProtocol(String name) {
		return protocols.get(name);
	}

	public static Iterator<String> getProtocolNames() {
		return protocols.keySet().iterator();
	}

	/**
	 * returns frame timing for a key of this protocol
	 * @param key
	 * @return
	 */
	public short[] getFrame(String key) {
		return keys.get(key);
	}

	protected IRProtocol(String name, JSONObject config)
			throws JSONException {
		this.name = name;
		frequency = (byte)config.getInt("frequency");
		cycle = (byte)config.getInt("cycle");
		period = (short)config.getInt("period");
		JSONArray prefix = null;
		int prefixLen;
		JSONArray suffix = null;
		int suffixLen;
		if (config.has("prefix")) {
			prefix = config.getJSONArray("prefix");
			prefixLen = prefix.length();
		} else {
			prefixLen = 0;
		}
		if (config.has("suffix")) {
			suffix = config.getJSONArray("suffix");
			suffixLen = suffix.length();
		} else {
			suffixLen = 0;
		}

		keys = new HashMap<String, short[]>();
		JSONObject keysJson = config.getJSONObject("keys");

		@SuppressWarnings("unchecked")
		Iterator<String> iter = keysJson.keys();
		while(iter.hasNext()) {
			String key = iter.next();
			JSONArray frameContent = keysJson.getJSONArray(key);
			int len = prefixLen + frameContent.length() + suffixLen;
			short[] frame = new short[len];
			if (prefixLen != 0) {
				fillArray(prefix, frame, 0);
			}
			fillArray(frameContent, frame, prefixLen);
			if (suffixLen != 0) {
				fillArray(suffix, frame, prefixLen + frameContent.length());
			}
			keys.put(key, frame);
		}
	}

	static private void fillArray(JSONArray src, short[] dst, int dstIndex)
			throws JSONException {
		for(int i = 0, j = dstIndex; i < src.length(); i++, j++) {
			dst[j] = (short)src.getInt(i);
		}
	}

	public String getName() {
		return name;
	}

	public byte getFrequency() {
		return frequency;
	}

	public byte getCycle() {
		return cycle;
	}

	public short getPeriod() {
		return period;
	}

	public String toString() {
		return "IRProtocol " + name;
	}
}
