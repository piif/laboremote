package net.atos.aw.tum;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class IRRemote {
	private int output;
	private String name;
	private String description;
	private IRModel model;
	private IRProtocol protocol;

	private static Map<String, IRRemote> remotes;

	/**
	 * load protocol list from json config
	 * @param config
	 * @throws JSONException
	 */
	public static void loadRemotes(JSONObject config)
			throws JSONException, ConfigException {
		remotes = new HashMap<String, IRRemote>();

		@SuppressWarnings("unchecked")
		Iterator<String> iter = config.keys();
		while(iter.hasNext()) {
			String name = iter.next();
			JSONObject remoteConfig = config.getJSONObject(name);
			IRRemote remote = new IRRemote(name, remoteConfig);
			remotes.put(name, remote);
		}
	}

	/**
	 * retrieve one of the remotes by its name
	 * @param name
	 * @return IRRemote
	 */
	public static IRRemote getRemote(String name) {
		return remotes.get(name);
	}

	public static Iterator<String> getRemoteNames() {
		return remotes.keySet().iterator();
	}

	public byte[] getCommand(String key) {
		// output + freq + period + proto.frame[key]
		short[] frame = protocol.getFrame(key);
		if (frame == null) {
			return null;
		}
		byte[] command = new byte[8 + frame.length*2 + 1];
		command[0] = 'S';
		command[1] = (byte)output;
		command[2] = protocol.getFrequency();
		command[3] = protocol.getCycle();
		command[4] = (byte)(protocol.getPeriod() & 0xff);
		command[5] = (byte)(protocol.getPeriod() >> 8);
		command[6] = (byte)(frame.length & 0xff);
		command[7] = (byte)(frame.length >> 8);
		for (int i = 0; i < frame.length; i++) {
			command[8 + i*2] = (byte)(frame[i] & 0xff);
			command[8 + i*2 + 1] = (byte)(frame[i] >> 8);
		}
		command[8 + frame.length*2] = 'F';

		return command;
	}

	protected IRRemote(String name, JSONObject config)
			throws JSONException, ConfigException {
		this.name = name;
		description = config.getString("description");
		if (description == null) {
			throw new ConfigException("missing description for remote " + name);
		}
		output = config.getInt("output");
		model = IRModel.getModel(config.getString("model"));
		if (model == null) {
			throw new ConfigException("unknown model " +
					config.getString("model") + " for remote " + name);
		}
		protocol = model.getProtocol();
	}

	public String toString() {
		return "IRRemote " + description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

}
