/*
 * Copyright 2011 Future Systems
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.future.linux.api.msgbus;

import java.io.IOException;

import kr.co.future.linux.api.Ipv6NeighborCache;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.apache.felix.ipojo.annotations.Component;

@Component(name = "linux-ipv6-neighbor-cache-plugin")
@MsgbusPlugin
public class Ipv6Plugin {
	@MsgbusMethod
	public void getNeighbors(Request req, Response resp) throws IOException { 
		resp.put("neighbors", Marshaler.marshal(Ipv6NeighborCache.getEntries()));
	}
}
