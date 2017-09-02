/*
 * Copyright 2011 Future Systems, Inc.
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
package kr.co.future.dom.msgbus;

import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.TimetableApi;
import kr.co.future.dom.model.Timetable;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

@Component(name = "dom-timetable-plugin")
@MsgbusPlugin
public class TimetablePlugin {
	@Requires
	private ConfigManager conf;

	@Requires
	private TimetableApi timetableApi;

	@MsgbusMethod
	public void getTimetables(Request req, Response resp) {
		resp.put("timetables", PrimitiveConverter.serialize(timetableApi.getTimetables(req.getOrgDomain())));
	}

	@MsgbusMethod
	public void getTimetable(Request req, Response resp) {
		String guid = req.getString("guid");
		Timetable timetable = timetableApi.getTimetable(req.getOrgDomain(), guid);
		resp.put("schedules", PrimitiveConverter.serialize(timetable));
	}

	@MsgbusMethod
	public void createTimetable(Request req, Response resp) {
		Timetable timetable = (Timetable) PrimitiveConverter.overwrite(new Timetable(), req.getParams(),
				conf.getParseCallback(req.getOrgDomain()));
		timetableApi.createTimetable(req.getOrgDomain(), timetable);
		resp.put("guid", timetable.getGuid());
	}

	@MsgbusMethod
	public void updateTimetable(Request req, Response resp) {
		Timetable before = timetableApi.getTimetable(req.getOrgDomain(), req.getString("guid"));
		Timetable timetable = (Timetable) PrimitiveConverter.overwrite(before, req.getParams(), conf.getParseCallback(req.getOrgDomain()));
		timetableApi.updateTimetable(req.getOrgDomain(), timetable);
	}

	@MsgbusMethod
	public void removeTimetable(Request req, Response resp) {
		String guid = req.getString("guid");
		timetableApi.removeTimetable(req.getOrgDomain(), guid);
	}
}
