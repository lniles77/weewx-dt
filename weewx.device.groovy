/*
The SmartThings end of an integration to pull weather information from an instance of weewx (http://weewx.com/)

Copyright 2017 Les Niles Les@2pi.org
This is don't-be-a-dick software.  Anyone is free to use and/or modify it for any purpose, at your own risk,
as long as this statement and license remain intact, no other license is claimed, and no other restrictions are
imposed on other users of the software.

No beer, free or otherwise, was harmed in the development of this software.

*/

metadata {
  preferences {
    input("ip", "string", title:"Weewx IP address", description:"numeric ip", required: true, displayDuringSetup: true)
    input("port", "string", title:"Weewx JSON port", description:"port #", required: true, displayDuringSetup: true)
    input("jsonPath", "string", title:"Weewx JSON path", description:"begin with '/'", required: true, displayDuringSetup: true)
  }

  definition(name:"Weewx", namespace:"lniles77", author: "Les Niles") {
    capability "Temperature Measurement"
    capability "Sensor"
    capability "Refresh"
    capability "Polling"

    attribute "obsTime", "string"
    attribute "wind", "number"
    attribute "windDir", "number"
    attribute "windVec", "string"
    attribute "gust", "number"
    attribute "gustDir", "number"
    attribute "gustVec", "string"
  }

  tiles {
    valueTile("observationTime", "device.obsTime", width:2, height:1) {
      state "default", label:'${currentValue}'
    }

    valueTile("temperature", "device.temperature", width:1, height:1) {
      state "temperature", label:'${currentValue}°F', unit: "°F"
    }

    valueTile("windVec", "device.windVec", inactiveLabel:false) {
      state "default", label:'Wind ${currentValue}'
    }

    valueTile("gustVec", "device.gustVec", inactiveLabel:false) {
      state "default", label:'Gust ${currentValue}'
    }

    standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
      state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
    }

    main "temperature"
    details(["observationTime", "temperature", "windVec", "gustVec", "refresh"])
  }
}


// Parse the response
def parse(description) {
  log.debug "parsing weewx data ${description}"

  def msg = parseLanMessage(description)

  def data = msg.json
  log.debug "parsed fields ${data} from ${msg}"
  
  if ( data ) {
    if ( data.containsKey("time") && data.time ) {
      log.debug "Observation Time ${data.time}"
      sendEvent(name: "obsTime", value: data.time)
    }

    if ( data.containsKey("outTemp") && data.outTemp ) {
      log.debug "temperature ${data.outTemp}"
      sendEvent(name: "temperature", value: data.outTemp as float)
    }

    if ( data.containsKey("windSpeed") && data.windSpeed ) {
      log.debug "wind ${data.windSpeed}"
      sendEvent(name: "wind", value: data.windSpeed as float)
      if ( data.containsKey("windDir") && data.windDir ) {
	log.debug "windVec"
	sendEvent(name: "windVec", value: "${data.windSpeed}mph from ${data.windDir}°")
      }
    }

    if ( data.containsKey("windDir") && data.windDir ) {
      log.debug "windDir ${data.windDir}"
      sendEvent(name: "windDir", value: data.windDir as float)
    }

    if ( data.containsKey("windGust") && data.windGust ) {
      log.debug "gust ${data.windGust}"
      sendEvent(name: "gust", value: data.windGust as float)
    }

    if ( data.containsKey("windGustDir") && data.windGustDir ) {
      log.debug "gustDir ${data.windGustDir}"
      sendEvent(name: "gustDir", value: data.windGustDir as float)
      if ( data.containsKey("windGust") && data.windGust ) {
	log.debug "gustVec"
	sendEvent(name: "gustVec", value: "${data.windGust}mph from ${data.windGustDir}°")
      }
    }

  }
}

def poll() {
  log.debug "Polling\n"
  getWeewxData()
}

def refresh() {
  log.debug "Refreshing\n"
  getWeewxData()
}

// 

private getWeewxData() {

  setNetworkId(ip, port)
  
  def params = [ method: "GET",
		 headers: getHeaders(),
		 path: jsonPath,
		 HOST: "${ip}:${port}"
	       ]

  def hubAct = new physicalgraph.device.HubAction( params, getNetworkId(ip, port) )
  log.debug "getWeewxData: ${params}, dni ${device.deviceNetworkId}"

  hubAct
}

private getHeaders() {
  [ Host: "${ip}:${port}" ]
}

private getNetworkId(addr, portnum) {
  String ah = addr.tokenize('.').collect { String.format('%02x', it.toInteger()) }.join()
  String ph = portnum.toString().format('%04x', portnum.toInteger())
  String dni = "$ah:$ph".toUpperCase()
  log.debug "Device Network ID is ${dni}"
  return dni
}

private setNetworkId(addr, portnum) {
  log.debug "setting Device Network ID"
  device.deviceNetworkId = getNetworkId(addr, portnum)
}

    
