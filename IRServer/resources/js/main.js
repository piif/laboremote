var config = {};
var currentRemotes = [];

console.info("main loaded");

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

function showRemote (id) {
	console.log("show remote " + id);
	// TODO : handle several remote simultaneously
	currentRemotes = [ id ];
	// TODO : several remotes => add new keys when adding one / remove specific ones when removing one
	var html = "<tr>", i = 0;
	for (var key in config.remotes[id].protocol.keys) {
		if ("k-" + key in standardButtons) {
			continue;
		}
		if (i && i % 4 === 0) {
			html += "</tr><tr>";
		}
		html += "<td id='k-" + key + "'>" + key + "</td>"
		i++;
	}
	html += "</tr>";
	$("#remote-other").html(html);
	$("#remote-other td").click(onKey);
	$("#remote").show();
	$("#remoteList").hide();
//	$.ajax({
//		url: config.remotes[id].model.map,
//		dataType: "text",
//		success: function( data ) {
//			console.log(data);
//		}
//	});
}

function onKey(event) {
	var key = $(this).attr("id").substring(2);
	var command = event.ctrlKey ? "R" : "S";
	console.log("HIT " + key + " for " + command + " on " + currentRemotes[0]);
	if (command === "S") {
		for (var i = 0; i < currentRemotes.length; i++) {
			if (!(key in config.remotes[currentRemotes[i]].protocol.keys)) {
				continue;
			}
			$.ajax({
				url: config.rootUrl + "/" + currentRemotes[i] + "/" + key
			});
		}
	} else {
		$.ajax({
			url: config.rootUrl + "/RECEIVE/" + currentRemotes[0] + " " + key
		});
	}
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
			config.rootUrl = location.protocol + "//" + location.hostname + ":" + config.web.port;
		},
		error: function (status) {
			console.error("Bad or unknown config ?");
			$("#loading").html("<b>Can't load config</b>");
		}
	});

	standardButtons = {};

	var buttons = $("#remote td[id*='k']");
	buttons.each(function() {
		standardButtons[$(this).attr("id")] = true;
	});
	buttons.click(onKey);

	$("#remote li").click(function () {
		$("#remote").hide();
		$("#remoteList").show();
	});
});
