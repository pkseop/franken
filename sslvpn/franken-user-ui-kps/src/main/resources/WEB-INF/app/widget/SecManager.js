define([
	"dojo/_base/declare",
	"dijit/_WidgetBase",
	"dijit/_TemplatedMixin",
	"dojo/text!./templates/SecManager.html",
	"dojox/grid/EnhancedGrid",
	"dojox/grid/enhanced/plugins/IndirectSelection",
	"dojo/store/Memory",
	"dojo/data/ObjectStore",
	"dojo/request",
	"dojo/parser",
	"dojo/domReady!"
], function(declare, _WidgetBase, _TemplatedMixin, template, EnhancedGrid, IndirectSelection, Memory, ObjectStore, request, parser, domReady){
	 		return declare([_WidgetBase, _TemplatedMixin], {
			
		// Our template - important!
		templateString: template,
		widgetsInTemplate: true,
		
		baseClass: 'secManager',
		
		secManagerGrid: null,
		
		postCreate: function(){
			this.drawSecManagerGrid();
			
			//IE8 doesn't support trim method.
			if(typeof String.prototype.trim !== 'function') {
			  String.prototype.trim = function() {
				return this.replace(/^\s+|\s+$/g, ''); 
			  }
			}
			
			console.log("postCreate finished");
		},
		
		drawSecManagerGrid: function() {	
	
			var thisSecManager = this;
			var tsTimeStamp= new Date().getTime();
			request.get("getUserList", {
				handleAs: "json",
				query: "time=" + tsTimeStamp
			}).then(function(data){
				store = new Memory({ data: data.Data });
				dataStore = new ObjectStore({ objectStore: store });
		
				//if 'var' would not be written then IE8 makes error.
				var secManagerGrid = new EnhancedGrid({
					store: dataStore,
					autoWidth: 'false',
					query: { Data: "*" },
					plugins: {indirectSelection: true},
					selectionMode:'single',
					structure: [
						{
							defaultCell: { width: "80px", styles: 'text-align: center;' },
							cells: [								
								{ name: "사번", field: "PERNR" },
								{ name: "성명", field: "ENAME" },
								{ name: "소속", field: "ORGTXT", width: "240px" },
								{ name: "직위", field: "JIKWINM" }								
							]
						}
					]
					
				}, thisSecManager.secManagerGridNode);
				
				// since we created this grid programmatically, call startup to render it
				secManagerGrid.startup();
				thisSecManager.secManagerGrid = secManagerGrid;
				thisSecManager.toggleManagerRow(secManagerGrid);
			});
			
		},
		
		toggleManagerRow: function(secManagerGrid) {
			for(var i = 0; ; i++) {
				var item = secManagerGrid.getItem(i);
				if(item == null)
					break;
				var value = secManagerGrid.store.getValue(item, "SECMANAGER");
				if(value == 'X') {						
					secManagerGrid.rowSelectCell.toggleRow(item, true);
					break;
				}
			}
		},
		
		search: function() {
			var pernr = dojo.byId('filterPernr').value.trim(), 
				ename = dojo.byId('filterEname').value.trim();
		  	
			if(pernr.length == 0)
				pernr = "*";
			if(ename.length == 0)
				ename = "*";
				
			this.secManagerGrid.filter({PERNR:pernr, ENAME:ename});
			this.secManagerGrid.startup();
		},
		
		secManagerChange: function() {
			var selectedRows= this.secManagerGrid.selection.getSelected();
			var id = this.secManagerGrid.store.getValue(selectedRows[0], "PERNR");
			
			var tsTimeStamp= new Date().getTime();
			request.get('setSecManager', {
				query: 'PERNR=' +id + "&time" + tsTimeStamp,
				handleAs: 'json'
			}).then(function(data) {
				if (data['Result'] == "Fail") {
					alert('정보보안 담당자 변경에 실패했습니다.');
					console.log(data['ResultMessage']);
				} else if(data['Result'] == "Success") {
					alert('정보보안 담당자가 변경되었습니다.');
					console.log(data['ResultMessage']);
				}
			}, function(error) {
				alert(error.stack);
			}, function(event) {
				
			});
		}
		
	});
});