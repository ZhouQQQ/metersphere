<template>
    <div>
      <el-form :model="commonConfig" :rules="rules" ref="commonConfig" :disabled="isReadOnly">

        <span>{{$t('api_test.environment.globalVariable')}}</span>
        <ms-api-scenario-variables :show-copy="false" :items="commonConfig.variables"/>

        <el-form-item>
          <el-switch v-model="commonConfig.enableHost" active-text="Hosts"/>
        </el-form-item>
        <ms-api-host-table v-if="commonConfig.enableHost" :hostTable="commonConfig.hosts" ref="refHostTable"/>
      </el-form>
    </div>
</template>

<script>
  import {CommonConfig, Environment} from "../../model/EnvironmentModel";
  import MsApiScenarioVariables from "../ApiScenarioVariables";
  import MsApiHostTable from "../ApiHostTable";

    export default {
      name: "MsEnvironmentCommonConfig",
      components: {MsApiHostTable, MsApiScenarioVariables},
      props: {
        commonConfig: new CommonConfig(),
        isReadOnly: {
          type: Boolean,
          default: false
        },
      },
      data() {
        return {
          rules: {

          },
        }
      },
      methods: {
        validate() {
          let isValidate = false;
          this.$refs['commonConfig'].validate((valid) => {
            if (valid) {
              // 校验host列表
              let valHost = true;
              if (this.commonConfig.enableHost) {
                for (let i = 0; i < this.commonConfig.hosts.length; i++) {
                  valHost = this.$refs['refHostTable'].confirm(this.commonConfig.hosts[i]);
                }
              }
              if (valHost) {
                isValidate = true;
              } else {
                isValidate = false;
              }
            } else {
              isValidate = false;
            }
          });
          return isValidate;
        }
      }
    }
</script>

<style scoped>

</style>
