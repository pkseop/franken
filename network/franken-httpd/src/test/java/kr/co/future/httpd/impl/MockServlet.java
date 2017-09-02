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
package kr.co.future.httpd.impl;

import javax.servlet.http.HttpServlet;

public class MockServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private int id;
	
	public MockServlet(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
