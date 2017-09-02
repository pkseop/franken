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
package kr.co.future.logdb;

import java.util.Collection;

public interface LookupHandlerRegistry {
	Collection<String> getLookupHandlerNames();

	LookupHandler getLookupHandler(String name);

	void addLookupHandler(String name, LookupHandler handler);

	void removeLookupHandler(String name);
}