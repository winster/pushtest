/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {
	regid: '',
    // Application Constructor
    initialize: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicity call 'app.receivedEvent(...);'
    onDeviceReady: function() {
        console.log('Received Event: onDeviceReady' );
        var appElement = document.querySelector('.app');
        var buttonbarElement = document.querySelector('.buttonbar');
        appElement.setAttribute('style', 'display:none;');
        buttonbarElement.setAttribute('style', 'display:block;');
        PushPlugin.init(app.initSuccess, app.initError);
    },
    initSuccess: function(msg){
    	console.log(msg);
    },
    initError: function(msg){
    	console.log(msg);
    },
    register: function(){
        if (device.platform.toLowerCase() == 'android') {
            PushPlugin.register(app.registerSuccess, app.registerError, {"senderID":"446023914210","ecb":"onNotification"});        // required!
        } else {
            PushPlugin.register(tokenHandler, errorHandler, {"badge":"true","sound":"true","alert":"true","ecb":"onNotificationAPN"});    // required!
        }
    },
    unregister: function(){
        PushPlugin.unregister(app.unregisterSuccess, app.unregisterError);
    },
    registerSuccess: function(data){
        console.log("inside registerSuccess "+data);
    },
    registerError: function(error){
        console.log("inside registerError "+error);  
    },
    registerWithServer: function() {
    	var httpRequest;
  		if (window.XMLHttpRequest) { // Mozilla, Safari, ...
      		httpRequest = new XMLHttpRequest();
    	}
	    httpRequest.onreadystatechange = app.onServerResponse.bind(null, httpRequest);
    	httpRequest.open('POST', "http://pushnotificationtest.herokuapp.com/register");
    	httpRequest.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    	httpRequest.send("regId="+encodeURIComponent(app.regid)+"&service=GCM");
    },
    onServerResponse: function(httpRequest) {
    	if (httpRequest.readyState === 4) {
      		var regElement = document.querySelector('.register');
			var elt = document.createElement("p"); 
			if (httpRequest.status === 200) {
        		elt.textContent = httpRequest.responseText;
			} else {
        		elt.textContent = 'There was a problem with the request.';
      		}
      		regElement.appendChild(elt);      		
    	}
  	},
  	unregisterSuccess: function(data){
  		console.log("inside unregisterSuccess "+data);
  	},
	unregisterError: function(error){
		console.log("inside unregisterError "+error);  
  	},
  	unregisterWithServer: function() {
    	var httpRequest;
  		if (window.XMLHttpRequest) { // Mozilla, Safari, ...
      		httpRequest = new XMLHttpRequest();
    	}
	    httpRequest.onreadystatechange = app.onServerResponseUnRegister.bind(null, httpRequest);
    	httpRequest.open('POST', "http://pushnotificationtest.herokuapp.com/unregister");
    	httpRequest.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    	httpRequest.send("regId="+encodeURIComponent(app.regid)+"&service=GCM");
    },
    onServerResponseUnRegister: function(httpRequest) {
    	if (httpRequest.readyState === 4) {
      		var unregElement = document.querySelector('.unregister');
			var elt = document.createElement("p"); 
			if (httpRequest.status === 200) {
        		elt.textContent = httpRequest.responseText;
			} else {
        		elt.textContent = 'There was a problem with the request.';
      		}
      		unregElement.appendChild(elt);      		
    	}
  	}  	
};
function onNotification(msg){
	console.log("inside onNotification"+msg);
	var regElement = document.querySelector('.register');
	var unregElement = document.querySelector('.unregister');
	var notiElement = document.querySelector('.notification');
	switch(msg.event ) {
		case 'register':
			app.regid = msg.regid;
			var elt = document.createElement("p"); 
			elt.textContent = msg.regid;
			regElement.appendChild(elt);
			app.registerWithServer();
			break;
		case 'unregister': 
			var elt = document.createElement("p"); 
			elt.textContent = msg.message;
			unregElement.appendChild(elt);
			app.unregisterWithServer();
			break;
		case 'message': 
			var elt = document.createElement("p"); 
			elt.textContent = msg.payload.title + '-' + msg.payload.ticker + '-' + msg.payload.content
						+ '-' + msg.payload.from + '-' + msg.payload.collapse_key + '-' + msg.payload.foreground + '-' + msg.payload.coldstart;
			notiElement.appendChild(elt);
			break;
		case 'error': 
			var elt = document.createElement("p"); 
			elt.textContent = msg.error;
			notiElement.appendChild(elt);
			break;
	}
}

window.onload = function(){
    var registerBtn = document.querySelector('#register');
    var unregisterBtn = document.querySelector('#unregister');
    registerBtn.addEventListener('click', app.register, false);
    unregisterBtn.addEventListener('click', app.unregister, false);      
    
    /*if (device.platform.toLowerCase() == 'android') {
        PushPlugin.register(app.registerSuccess, app.registerError, {"senderID":"446023914210","ecb":"onNotification"});        // required!
    } else {
        //pushNotification.register(tokenHandler, errorHandler, {"badge":"true","sound":"true","alert":"true","ecb":"onNotificationAPN"});    // required!
    }*/
};