// config items to load from json files
var config = {
	protocols: {},
	models: {},
	remotes: {},
    keymap: {}
};
// destination remote
var currentRemote = null;

console.info("main loaded");

// load a config file
function loadConfig (name) {
	if (config.resources && config.resources[name]) {
		config.waiting++;
		$.ajax({
			url: config.resources[name],
			success: function (data) { configLoaded(name, data); },
			error: function (status) { configFailed(name, status); },
		});
	} else {
		console.error("config.resources." + name + " missing");
	}
}
// callback for config loading
function configLoaded (name, data) {
	console.info("file " + name + " loaded " + config.waiting);
	config[name] = data;
	config.waiting--;
	if (config.waiting === 0) {
		setRemoteList();
	}
}

function configFailed (name, status) {
	console.error("file " + name + " failed", status);
}

// initialize remote list
function setRemoteList () {
	var html = "";
	for (var name in config.remotes) {
		config.remotes[name].model = config.models[config.remotes[name].model];
		config.remotes[name].protocol = config.protocols[config.remotes[name].model.protocol];

		html += "<li rel='" + name + "'>" +
			config.remotes[name].description + "</li>\n";
	}
	$("#loading").hide();
	$("#remoteList")
		.html("<ul>" + html + "</ul>")
		.show();

	$("#remoteList li").click(function () {
		showRemote($(this).attr("rel"));
	});
}

// show one remote
function showRemote (id) {
	console.log("show remote " + id);
	// TODO : handle several remote simultaneously
	currentRemote = id;
	var html = "<div>", i = 0;
	var map = config.remotes[id].model.map;
	for (var i = 0; i < map.length; i++) {
		if (map[i] === "") {
			html += "</div>\n<div>";
		} else {
			html += "<span id='k-" + map[i] + "'><img onError=\"replace('" + map[i] + "')\" src='icons/" + map[i] + ".png'/></span>"
		}
	}
	html += "</div>\n";

	$("#remote-name").html(config.remotes[id].description);
	$("#remote-keys").html(html);
	$("#remote-keys span").click(onClick);
	$("#remote").show();
	$("#remoteList").hide();
	$(window).keypress(onKey);
//	$.ajax({
//		url: config.remotes[id].model.map,
//		dataType: "text",
//		success: function( data ) {
//			console.log(data);
//		}
//	});
}

function replace (id) {
	$("#k-" + id).html(id);
}

function onClick(event) {
	var key = $(this).attr("id").substring(2);
	console.log("HIT " + key + " on " + currentRemote);

	send(key);
}
function onKey(event) {
	var key = null;
	if (event.charCode != 0) {
		key = String.fromCharCode(event.charCode);
	} else {
		switch(event.keyCode) {
		case KeyEvent.DOM_VK_UP:
			key = "up";
		break;
		case KeyEvent.DOM_VK_DOWN:
			key = "down";
		break;
		case KeyEvent.DOM_VK_LEFT:
			key = "left";
		break;
		case KeyEvent.DOM_VK_RIGHT:
			key = "right";
		break;
		case KeyEvent.DOM_VK_RETURN:
			key = "enter";
		break;
		case KeyEvent.DOM_VK_BACK_SPACE:
			key = "back";
		break;
		}
	}
	console.log("HIT " + key + " on " + currentRemote);
	event.stopPropagation();
	if (key in config.keymap) {
		event.preventDefault();
		send(config.keymap[key]);
	}
}
function send(key) {
	if (!(key in config.remotes[currentRemote].protocol.keys)) {
		return;
	}
	$.ajax({
		url: config.rootUrl + "/" + currentRemote + "/" + key
	});
}

$(document).ready(function () {
	console.info("init");
	var configExpr = /config=(\w+)/.exec(location.search);
	var configName;
	if (configExpr) {
		configName = configExpr[1];
	} else {
		configName = "config";
	}
	$.ajax({
		url: configName + ".json",
		success: function (data) {
			if (!data.resources) {
				console.error("Bad config ?");
				return;
			}
			console.info("config loaded");
			config = data;
			config.waiting = 0;
			loadConfig("models");
			loadConfig("remotes");
			loadConfig("protocols");
			loadConfig("keymap");
			config.rootUrl = location.protocol + "//" + location.hostname + ":" + config.web.port;
		},
		error: function (status) {
			console.error("Bad or unknown config ?");
			$("#loading").html("<b>Can't load config</b>");
		}
	});

	// list of buttons common to all remotes
	standardButtons = {};

	var buttons = $("#remote td[id*='k']");
	buttons.each(function() {
		standardButtons[$(this).attr("id")] = true;
	});
	buttons.click(onClick);

	// bind "back to list" button
	$("#remote li").click(function () {
		$("#remote").hide();
		$("#remoteList").show();
		currentRemote = null;
	});
});
