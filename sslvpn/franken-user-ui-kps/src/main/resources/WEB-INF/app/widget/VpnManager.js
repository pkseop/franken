define([
	"dojo/_base/declare",
	"dijit/_WidgetBase",
	"dijit/_TemplatedMixin",
	"dojo/text!./templates/VpnManager.html",
	"dojo/store/JsonRest",	
	"dojox/grid/EnhancedGrid",
	"dojox/grid/enhanced/plugins/Pagination",
	"dojo/store/Memory",
	"dojo/store/Cache",
	"dojo/data/ObjectStore",
	"dojo/request",
	"dijit/registry",
	"dojo/parser",
	"js/base64",
	"dojo/domReady!"
], function(declare, _WidgetBase, _TemplatedMixin, template, JsonRest, EnhancedGrid, Pagination,  Memory, Cache, ObjectStore, request, registry, parser, base64, domReady){
	 		return declare([_WidgetBase, _TemplatedMixin], {
		
		// Our template - important!
		templateString: template,
		widgetsInTemplate: true,
		
		baseClass: 'vpnManager',
		
		resultGrid: null,
		approvalGrid: null,
		dataStore: null,
		
		postCreate: function(){
			this.inherited(arguments);
			this.drawResultGrid();
			this.drawApprovalGrid();
			
			//IE8 doesn't support trim method.
			if(typeof String.prototype.trim !== 'function') {
			  String.prototype.trim = function() {
				return this.replace(/^\s+|\s+$/g, ''); 
			  }
			}
			
			console.log("postCreate finished");
		},
    
		drawResultGrid: function() {
			var thisVpnManager = this;
			var tsTimeStamp= new Date().getTime();
			
			memoryStore = new Memory();
			restStore = new JsonRest({
				target: "getVpnUserList?PERNR=2070000&time="+ 	tsTimeStamp,
				idProperty: "PERNR"
			});
			
			cacheStore = new Cache(restStore, memoryStore)
            dataStore = new ObjectStore({ objectStore: cacheStore });
            this.dataStore = dataStore;
			
			//if 'var' would not be written then IE8 makes error.
			var resultGrid = new EnhancedGrid({
				store: dataStore,
				autoWidth: 'false',					
				structure: [
					{
						defaultCell: { width: "75px", styles: 'text-align: center;' },
						cells: [
						    { name: "신청일자", field: "REQUEST_DATE", width: "100px" },
							{ name: "신청차수", field: "REQUEST_DEGREE", width: "60px"},
							{ name: "사번", field: "PERNR" },
							{ name: "성명", field: "ENAME" },
							{ name: "소속", field: "ORGTXT", width: "220px" },
							{ name: "직위", field: "JIKWINM" },							
							{ name: "신청현황", field: "DECISION_STATUS", width: "130px" }
						]
					}
				],
				plugins: {
				  pagination: {
					  pageSizes: ["15", "25", "50", "75", "100", "All"],
					  description: true,
					  sizeSwitch: true,
					  pageStepper: true,
					  gotoButton: true,
							  /*page step to be displayed*/
					  //maxPageStep: 5,
							  /*position of the pagination bar*/
					  position: "bottom",
					  defaultPageSize: 15
				  }
				}									
			}, thisVpnManager.resultGridNode);
			
			// since we created this grid programmatically, call startup to render it
			resultGrid.startup();
			// click event to display data. 
			resultGrid.on("RowClick", function(evt){
				var idx = evt.rowIndex,
					rowData = resultGrid.getItem(idx);
				thisVpnManager.getDetailDataById(rowData.PERNR, rowData.REQUEST_DEGREE);
			}, true);
			thisVpnManager.resultGrid = resultGrid;
			//disable sort
			thisVpnManager.resultGrid.canSort=function(){return false};	
		},
		
		drawApprovalGrid: function() {
			this.approvalGrid = new EnhancedGrid({
				autoWidth: 'false',
				structure: [
					{
						defaultCell: { width: "80px", styles: 'text-align: center;' },
						cells: [
							{ name: "구분", field: "DECISION_ID", width: "45px" },
							{ name: "성명", field: "DECISION_ENAME" },
							{ name: "소속", field: "DECISION_ORGTXT", width: "230px" },
							{ name: "직위", field: "DECISION_JIKWINM", width: "108px" },
							{ name: "결재/반려", field: "DECISION_RESULT" },
							{ name: "결재/반려일자", field: "DECISION_DATE", width: "195px" }
						]
					}
				]									
			}, this.approvalGridNode);
			//disable sort
			this.approvalGrid.canSort=function(){return false};
		},
		
		getDetailDataById: function(id, degree) {
			var thisVpnManager = this;
			var tsTimeStamp= new Date().getTime();
			request.get("getVpnUserInfo", {
				handleAs: "json",
				query: "PERNR=" + id + "&DEGREE=" + degree +"&time=" + 	tsTimeStamp			
			}).then(function(data){
				var userInfo = data.Data;
				thisVpnManager.setValueToInputWidgets(userInfo);
				
				if(userInfo.DECISION_REQUEST > 0)
					thisVpnManager.setStoreOfApprovalGrid(userInfo.DECISION_LIST);
				else
					thisVpnManager.emptyStoreOfGrid(thisVpnManager.approvalGrid);
			});
			
		},
		
		setValueToInputWidgets: function(userInfo) {
			var nodeENAME = registry.byId("ENAME");
			nodeENAME.set("value", userInfo.ENAME);				
			var nodeORGTXT = registry.byId("ORGTXT");
			nodeORGTXT.set("value", userInfo.ORGTXT);				
			var nodeSTART_DATE = registry.byId("START_DATE");
			nodeSTART_DATE.set("displayedValue", userInfo.START_DATE);				
			var nodeEND_DATE = registry.byId("END_DATE");
			nodeEND_DATE.set("displayedValue", userInfo.END_DATE);				
			var nodeREQUEST_REASON = registry.byId("REQUEST_REASON");
			nodeREQUEST_REASON.set("value", userInfo.REQUEST_REASON);				
			var nodeRELEVANT_BASE = registry.byId("RELEVANT_BASE");
			nodeRELEVANT_BASE.set("value", userInfo.RELEVANT_BASE);
		},
				
		setStoreOfApprovalGrid: function(DECISION_LIST) {
			var store = new Memory({ data: DECISION_LIST });
			var dataStore = new ObjectStore({ objectStore: store });
			this.approvalGrid.setStore(dataStore);
		},
		
		emptyStoreOfGrid: function(dataGrid) {
			var store = new Memory({ data: [] });
			var dataStore = new ObjectStore({ objectStore: store });
			dataGrid.setStore(dataStore);
		},
		
		getDecisionStatus: function(decisionStatus) {
			if(decisionStatus == "1차 승인 대기")
				return "0";
			else if(decisionStatus == "2차 승인 대기")
				return "1";
			else if(decisionStatus == "3차 승인 대기")
				return "2";
			else if(decisionStatus == "최종 승인 대기")
				return "3";
			else if(decisionStatus == "계정생성완료")
				return "4";
			else if(decisionStatus == "1차 승인 반려")
				return "5";
			else if(decisionStatus == "2차 승인 반려")
				return "6";
			else if(decisionStatus == "3차 승인 반려")
				return "7";
			else if(decisionStatus == "최종 승인 반려")
				return "8";
		},
		
		search: function() {
			var decisionStatus = dojo.byId('filterDecisionStatus').innerText.trim(), 
				requestDegree = dojo.byId('filterRequestDegree').value.trim();
				orgtxt = dojo.byId('filterOrgtxt').value.trim(), 
				pernr = dojo.byId('filterPernr').value.trim(), 
				ename = dojo.byId('filterEname').value.trim();
			
		  	var query = "";
			if(decisionStatus.trim() != '모두') {
				query += "DECISION_STATUS=" + this.getDecisionStatus(decisionStatus);
			}
			if(requestDegree.length > 0) {
				if(query.length > 0)
					query += "&";
				query += "REQUEST_DEGREE=" + requestDegree;
			}
			if(orgtxt.length > 0) {
				if(query.length > 0)
					query += "&";
				query += "ORGTXT=" + encodeURIComponent(Base64.encode(orgtxt));
			}
			if(pernr.length > 0) {
				if(query.length > 0)
					query += "&";
				query += "PERNR=" + pernr;
			}
			if(ename.length > 0) {
				if(query.length > 0)
					query += "&";
				query += "ENAME=" + encodeURIComponent(Base64.encode(ename));
			}
			
			if(query.length == 0) {
				this.resultGrid.setStore(this.dataStore);
				this.resultGrid.startup();
			} else {
				memoryStore = new Memory();
				restStore = new JsonRest({
					target: "searchVpnUserList?"+ query,
					idProperty: "PERNR"
				});
				
				cacheStore = new Cache(restStore, memoryStore)
				searchDataStore = new ObjectStore({ objectStore: cacheStore });
				
				this.resultGrid.setStore(searchDataStore);
				this.resultGrid.startup();
			}
			
		}
		
	});		
});