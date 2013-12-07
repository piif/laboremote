package net.atos.aw.tum;

import java.util.*;
import org.json.*;

public class IRModel {
	private String name;
	private IRProtocol protocol;

	private static Map<String, IRModel> models;

	// list describing remote keys, from top left to bottom right
	// "" entries mark line changes
	String[] map;
	// same list without "" markers
	String[] shortMap;

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
		JSONArray jsonMap = config.getJSONArray("map");
		map = new String[jsonMap.length()];
		String[] map2 = new String[jsonMap.length()];
		int j = 0;
		for (int i = 0; i < map.length; i++) {
			String k = jsonMap.getString(i);
			map[i] = k;
			if (k.length() != 0) {
				map2[j] = k;
				j++;
			}
		}
		shortMap = new String[j];
		System.arraycopy(map2, 0, shortMap, 0, j);
	}

	public IRProtocol getProtocol() {
		return protocol;
	}

	public String toString() {
		return "IRModel " + name;
	}
}
