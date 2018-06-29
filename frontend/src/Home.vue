<template>
<div>
    <b-row>
        <b-col>
            <b-table striped hover fixed :items="items" :fields="fields">
            <template slot="name" slot-scope="row">
              <div class="text-truncate" v-b-tooltip.hover.top="row.item.name" >{{row.item.name}}</div>
            </template>
            <template slot="state" slot-scope="row">
              <b-progress :value="100" variant="secondary" :animated="true" class="mb-3" v-show="row.item.loading"></b-progress>
              <div v-show="!row.item.loading">{{row.item.state}}</div>
            </template>
            <template slot="actions" slot-scope="row">
                <b-dropdown size="sm" text="Choose here">
                  <b-dropdown-item :disabled="row.item.state === 'Recording'" @click="start(row.item)">Start</b-dropdown-item>
                  <b-dropdown-item :disabled="row.item.state === 'Not recording'" @click="stop(row.item)">Stop</b-dropdown-item>
                  <b-dropdown-item :disabled="row.item.state === 'Not recording'" @click="dump(row.item)">Dump</b-dropdown-item>
                  <b-dropdown-item size="sm" :disabled="!row.item.hasDump" @click="saveDump(row.item)">Save</b-dropdown-item>
                </b-dropdown>
            </template>
            <template slot="view" slot-scope="row">
              <flamegraph-dropdown :item="row.item" :disabled="!row.item.hasDump"></flamegraph-dropdown>
            </template>
            </b-table>
        </b-col>
    </b-row>
    <b-row class="text-left">
        <b-col>
          <a :href="`#/dumps`">All local dumps</a>
           | 
          <a :href="`#/saves`">All remote storage dumps</a>
           | 
          <a :href="`#/upload`">Upload file</a>
        </b-col>
    </b-row>
</div>
</template>
<script>
import SavesMixin from "./SavesMixin";
export default {
  name: "home",
  mixins: [SavesMixin],
  data () {
    return {
      fields: [
        {
          key: "pid",
          label: "PID",
          sortable: true
        },
        {
          key: "name",
          label: "Process name",
          sortable: true
        },
        {
          key: "state",
          label: "State",
          sortable: true
        },
        {
          key: "recording",
          label: "Recording #"
        },
        "actions",
        "view"
      ]
    };
  },
  props: ["items"],
  methods: {
    start: function (item) {
      // Needs to call this.$set because loading is being dinamically added
      // and not reactive by default.
      this.$set(item, "loading", true);
      this.$http
        .post("/api/start/" + item.pid)
        .then(response => {
          item.state = response.data.state;
          item.recording = response.data.recording;
          item.loading = false;
        })
        .catch(e => {
          this.notifyError(e);
        });
    },
    stop: function (item) {
      this.$set(item, "loading", true);
      this.$http
        .post("/api/stop/" + item.pid + "/" + item.recording)
        .then(response => {
          item.state = response.data.state;
          item.loading = false;
        })
        .catch(e => {
          this.notifyError(e);
        });
    },
    dump: function (item) {
      this.$set(item, "loading", true);
      this.$http
        .post("/api/dump/" + item.pid + "/" + item.recording)
        .then(response => {
          this.$set(item, "hasDump", true);
          item.loading = false;
        })
        .catch(e => {
          this.notifyError(e);
        });
    }
  },
  mounted () {
    // Scroll up
    window.scrollTo(0, 0);
  }
};
</script>
