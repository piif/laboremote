package net.atos.aw.tum;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class IRModel {
	private String name;
	private IRProtocol protocol;

	private static Map<String, IRModel> models;

	/**
	 * load model list from json config
	 * @param config
	 * @throws JSONException
	 */
	public static void loadModels(JSONObject config)
			throws JSONException, ConfigException {
		models = new HashMap<String, IRModel>();

		@SuppressWarnings("unchecked")
		Iterator<String> iter = config.keys();
		while(iter.hasNext()) {
			String name = iter.next();
			JSONObject modelConfig = config.getJSONObject(name);
			IRModel model = new IRModel(name, modelConfig);
			models.put(name, model);
		}
	}

	/**
	 * retrieve one of the models by its name
	 * @param name
	 * @return IRModel
	 */
	public static IRModel getModel(String name) {
		return models.get(name);
	}


	protected IRModel(String name, JSONObject config)
			throws JSONException, ConfigException {
		this.name = name;
		String proto = config.getString("protocol");
		protocol = IRProtocol.getProtocol(proto);
		if (protocol == null) {
			throw new ConfigException("unknown protocol for model " + name);
		}
	}

	public IRProtocol getProtocol() {
		return protocol;
	}

	public String toString() {
		return "IRModel " + name;
	}
}
