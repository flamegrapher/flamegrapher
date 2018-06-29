<template>
    <div>
        <b-row align-h="center" align-v="center">
            <flower-spinner :animation-duration="2500" :size="100" color="#b95e42" v-show="loading"/>
        </b-row>
        <b-row align-h="start" v-show="!loading">
            <b-col cols="8">
                <b-form size="sm" inline @submit="search()" @reset="clear()">
                    <b-input-group>
                        <b-input-group-prepend>
                            <b-btn size="sm" @click="search()">Search</b-btn>
                        </b-input-group-prepend>

                        <b-form-input size="sm" type="text" v-model="searchExpression"></b-form-input>                    
                        
                        <b-input-group-append>
                            <b-btn size="sm" variant="link" @click="reset()">Reset zoom</b-btn>
                            <b-btn size="sm" variant="link" @click="clear()">Clear</b-btn>
                            <b-btn size="sm" variant="link" @click="saveToStorage()">Save to storage</b-btn>
                        </b-input-group-append>
                    </b-input-group>
                </b-form>
            </b-col>
        </b-row>
        <b-row v-show="!loading">
            <b-col>
                <div id="chart"></div>
            </b-col>
        </b-row>
        <back></back>        
    </div>
</template>
<script>

import { flamegraph } from "d3-flame-graph";
import { select } from "d3-selection";
import "d3-flame-graph/dist/d3-flamegraph.css";
import SavesMixin from "./SavesMixin";

export default {
  name: "flames",
  mixins: [SavesMixin],
  data () {
    return {
      chartState: [],
      searchExpression: "",
      loading: true
    };
  },
  methods: {
    search: function () {
      this.chartState.search(this.searchExpression);
    },
    reset: function () {
      this.chartState.resetZoom();
    },
    clear: function () {
      this.searchExpression = "";
      this.chartState.clear();
    },
    saveToStorage: function () {
      this.saveFlames(this.$route.params);
    }
  },
  mounted () {
    const chart = flamegraph()
            .width(1110)
            .cellHeight(18)
            .transitionDuration(750)
            .sort(false)
            .title("");

    this.chartState = chart;
    const params = this.$route.params;
    if (params.flameJsonData !== undefined) {
      select(`#chart`)
            .datum(response.flameJsonData)
            .call(chart);
      this.loading = false;
    } else {
      const url = "/api/flames/" + params.eventType + "/" + params.pid + "/" + params.recording;
      this.$http.get(url)
              .then(response => {
                select(`#chart`)
                      .datum(response.data)
                      .call(chart);
                this.loading = false;
              }).catch(error => {
                this.$notify({
                  title: error.message,
                  text: error.response.data,
                  type: "error",
                  duration: 10000
                });
              });
    }
  }
};
</script>
