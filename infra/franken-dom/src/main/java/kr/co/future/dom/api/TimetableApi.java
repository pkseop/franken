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
package kr.co.future.dom.api;

import java.util.Collection;

import kr.co.future.dom.model.Timetable;

import kr.co.future.dom.api.EntityEventProvider;

public interface TimetableApi extends EntityEventProvider<Timetable>{
	Collection<Timetable> getTimetables(String domain);

	Timetable findTimetable(String domain, String guid);

	Timetable getTimetable(String domain, String guid);

	void createTimetables(String domain, Collection<Timetable> timetables);

	void createTimetable(String domain, Timetable timetable);

	void updateTimetables(String domain, Collection<Timetable> timetables);

	void updateTimetable(String domain, Timetable timetable);

	void removeTimetables(String domain, Collection<String> guids);

	void removeTimetable(String domain, String guid);
}
