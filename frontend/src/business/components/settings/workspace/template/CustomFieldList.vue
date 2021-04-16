<template>
  <el-card>

    <template v-slot:header>
      <ms-table-header :is-tester-permission="true" :condition.sync="condition" @search="getCustomFields" @create="handleCreate"
                       :create-tip="'创建字段'" :title="'自定义字段'"/>
    </template>

    <ms-table
      v-loading="result.loading"
      :data="tableData"
      :condition="condition"
      :total="total"
      :page-size.sync="pageSize"
      :operators="operators"
      @handlePageChange="getCustomFields"
      @refresh="getCustomFields">

        <ms-table-column
          :label="'名称'"
          :fields="fields"
          prop="name">
          <template v-slot="scope">
            <span v-if="scope.row.system">
              {{$t(scope.row.name)}}
            </span>
            <span v-else>
              {{scope.row.name}}
            </span>
          </template>
        </ms-table-column>

        <ms-table-column
          :label="'使用场景'"
          :fields="fields"
          :filters="sceneFilters"
          prop="scene">
          <template v-slot="scope">
            <span>{{ sceneMap[scope.row.scene] }}</span>
          </template>
        </ms-table-column>

        <ms-table-column
          :label="'属性类型'"
          :fields="fields"
          :filters="fieldFilters"
          prop="type">
          <template v-slot="scope">
            <span>{{ fieldTypeMap[scope.row.type] }}</span>
          </template>
        </ms-table-column>

        <ms-table-column
          :label="'系统字段'"
          :fields="fields"
          prop="system">
          <template v-slot="scope">
            <span v-if="scope.row.system">
              是
            </span>
            <span v-else>
              否
            </span>
          </template>
        </ms-table-column>

        <ms-table-column
          :label="'备注'"
          :fields="fields"
          prop="remark">
        </ms-table-column>

        <ms-table-column
          sortable
          :label="'创建时间'"
          :fields="fields"
          prop="createTime">
          <template v-slot="scope">
            <span>{{ scope.row.createTime | timestampFormatDate }}</span>
          </template>
        </ms-table-column>

        <ms-table-column
          sortable
          :label="'更新时间'"
          :fields="fields"
          prop="updateTime">
          <template v-slot="scope">
            <span>{{ scope.row.updateTime | timestampFormatDate }}</span>
          </template>
        </ms-table-column>

    </ms-table>

    <ms-table-pagination :change="getCustomFields" :current-page.sync="currentPage" :page-size.sync="pageSize" :total="total"/>

    <custom-field-edit
      @refresh="getCustomFields"
      ref="customFieldEdit">

    </custom-field-edit>
  </el-card>
</template>

<script>
import MsTable from "@/business/components/common/components/table/MsTable";
import {getCurrentWorkspaceId} from "@/common/js/utils";
import MsTableColumn from "@/business/components/common/components/table/Ms-table-column";
import {CUSTOM_FIELD_LIST} from "@/common/js/default-table-header";
import MsTableOperators from "@/business/components/common/components/MsTableOperators";
import MsTableButton from "@/business/components/common/components/MsTableButton";
import CustomFieldEdit from "@/business/components/settings/workspace/template/CustomFieldEdit";
import MsTablePagination from "@/business/components/common/pagination/TablePagination";
import {
  CUSTOM_FIELD_SCENE_OPTION,
  CUSTOM_FIELD_TYPE_OPTION,
  FIELD_TYPE_MAP,
  SCENE_MAP
} from "@/common/js/table-constants";
import MsTableHeader from "@/business/components/common/components/MsTableHeader";
export default {
  name: "CustomFieldList",
  components: {
    MsTableHeader,
    MsTablePagination, CustomFieldEdit, MsTableButton, MsTableOperators, MsTableColumn, MsTable},
  data() {
    return {
      tableData: [],
      condition: {},
      total: 0,
      pageSize: 10,
      currentPage: 1,
      result: {},
      operators: [
        {
          tip: this.$t('commons.edit'), icon: "el-icon-edit",
          exec: this.handleEdit
        }, {
          tip: this.$t('commons.copy'), icon: "el-icon-copy-document", type: "success",
          exec: this.handleCopy,
          isDisable: this.systemDisable
        }, {
          tip: this.$t('commons.delete'), icon: "el-icon-delete", type: "danger",
          exec: this.handleDelete,
          isDisable: this.systemDisable
        }
      ],
    };
  },
  activated() {
    this.getCustomFields();
  },
  computed: {
    fields() {
      return CUSTOM_FIELD_LIST;
    },
    fieldFilters() {
      return CUSTOM_FIELD_TYPE_OPTION;
    },
    sceneFilters() {
      return CUSTOM_FIELD_SCENE_OPTION;
    },
    fieldTypeMap() {
      return FIELD_TYPE_MAP;
    },
    sceneMap() {
      return SCENE_MAP;
    }
  },
  methods: {
    getCustomFields() {
      this.condition.workspaceId = getCurrentWorkspaceId();
      this.result = this.$post('custom/field/list/' + this.currentPage + '/' + this.pageSize,
        this.condition, (response) => {
        let data = response.data;
        this.total = data.itemCount;
        this.tableData = data.listObject;
      });
    },
    handleEdit(data) {
      this.$refs.customFieldEdit.open(data);
    },
    handleCreate() {
      this.$refs.customFieldEdit.open();
    },
    handleCopy(data) {
      let copyData = {};
      Object.assign(copyData, data);
      copyData.id = null;
      copyData.name = data.name + '_copy';
      this.$refs.customFieldEdit.open(copyData);
    },
    handleDelete(data) {
      this.result = this.$get('custom/field/delete/' + data.id,  () => {
        this.$success(this.$t('commons.delete_success'));
        this.getCustomFields();
      });
    },
    systemDisable(row) {
      if (row.system) {
        return true;
      }
      return false;
    }
  }
};
</script>

<style scoped>

</style>