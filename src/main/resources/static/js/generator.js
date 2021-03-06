$(function () {
  $("#jqGrid").jqGrid({
    url: 'sys/generator/list',
    datatype: "json",
    colModel: [
      {label: '表名', name: 'tableName', width: 100, key: true},
      {label: 'Engine', name: 'engine', width: 70},
      {label: '表备注', name: 'tableComment', width: 100},
      {label: '创建时间', name: 'createTime', width: 100}
    ],
    viewrecords: true,
    height: 485,
    rowNum: 10,
    rowList: [10, 30, 50, 100, 200],
    rownumbers: true,
    rownumWidth: 25,
    autowidth: true,
    multiselect: true,
    pager: "#jqGridPager",
    jsonReader: {
      root: "page.list",
      page: "page.currPage",
      total: "page.totalPage",
      records: "page.totalCount"
    },
    prmNames: {
      page: "page",
      rows: "limit",
      order: "order"
    },
    gridComplete: function () {
      //隐藏grid底部滚动条
      $("#jqGrid").closest(".ui-jqgrid-bdiv").css({"overflow-x": "hidden"});
    }
  });
});

var vm = new Vue({
  el: '#rrapp',
  data: {
    tableName: null,
    q: {
      packageName: 'com.tfx0one.module.sys',
      tablePrefix: 'sys',
      moduleName: 'sys'
    }
  },
  methods: {
    query: function () {
      $("#jqGrid").jqGrid('setGridParam', {
        postData: {
          'tableName': vm.tableName
        },
        page: 1
      }).trigger("reloadGrid");
    },
    generator: function () {
      var tableNames = getSelectedRows();
      if (tableNames == null) {
        return;
      }
      if (!this.q.moduleName) {
        alert("请填写模块名！")
        return;
      }
      if (!this.q.tablePrefix) {
        alert("请填写表前缀！")
        return;
      }

      let qs = Object.keys(this.q).map((k) => `${k}=${this.q[k]}`).join('&')

      location.href = `sys/generator/code?${qs}&tables=${tableNames.join()}`;
    }
  }
});

