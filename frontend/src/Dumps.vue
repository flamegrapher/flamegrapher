<template>
    <div>
        <b-row align-h="center" align-v="center">
            <flower-spinner :animation-duration="2500" :size="100" color="#b95e42" v-show="loading"/>
        </b-row>
        <b-row>
          <b-col>
              <b-table striped hover fixed :items="items" :fields="fields" v-show="!loading">
              <template slot="actions" slot-scope="row">
                <b-btn size="sm" :href="`/api/dump/${row.item.pid}.${row.item.recording}.jfr`">Download</b-btn>
                <b-btn size="sm" @click="saveDump(row.item)">Save to storage</b-btn>
                <flamegraph-dropdown :item="row.item"></flamegraph-dropdown>
              </template>
              </b-table>
          </b-col>
        </b-row>
        <back></back>
    </div>
</template>
<script>
import SavesMixin from "./SavesMixin";
export default {
  name: "dumps",
  mixins: [SavesMixin],
  data () {
    return {
      loading: true,
      fields: [
        {
          key: "pid",
          lable: "PID",
          sortable: true
        },
        {
          key: "recording",
          label: "Recording #",
          sortable: true
        },
        "actions"
      ],
      items: []
    };
  },
  mounted () {
    const url = "/api/dumps/";
    this.$http
      .get(url)
      .then(response => {
        this.items = response.data;
        this.loading = false;
      })
      .catch(e => {
        this.notifyError(e);
      });
  }
};
</script>
