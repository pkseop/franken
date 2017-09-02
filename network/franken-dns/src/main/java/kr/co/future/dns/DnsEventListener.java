/*
 * Copyright 2012 Future Systems
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
package kr.co.future.dns;

import java.net.DatagramPacket;

public interface DnsEventListener {
	void onReceive(DatagramPacket queryPacket, DnsMessage query);

	void onSend(DatagramPacket queryPacket, DnsMessage query, DatagramPacket responsePacket, DnsMessage response);

	void onError(DatagramPacket packet, Throwable t);

	void onDrop(DatagramPacket queryPacket, DnsMessage query, Throwable t);
}
