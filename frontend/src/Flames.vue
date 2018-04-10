<template>
    <div>
        <b-row align-h="start">
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
                        </b-input-group-append>
                    </b-input-group>
                </b-form>
            </b-col>
        </b-row>
        <b-row>
            <b-col>
                <div id="chart"></div>
                <div id="details"></div>
            </b-col>
        </b-row>
        <back></back>        
    </div>
</template>
<script>

import axios from "axios";
import { flamegraph } from "d3-flame-graph";
import { select } from "d3-selection";
import "d3-flame-graph/dist/d3-flamegraph.css";

export default {
  name: "flames",
  data () {
    return {
      chartState: [],
      searchExpression: ""
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
    }
  },
  mounted () {
    const chart = flamegraph()
            .width(1110)
            .cellHeight(18)
            .transitionDuration(750)
            .sort(true)
            .title("");

    var details = document.getElementById("details");
    chart.details(details);
    this.chartState = chart;
        // const url = "/flame/api/flames/" + this.$route.params.pid + "/" + this.$route.params.recordingId;
    const url = "/stacks.json";
    axios.get(url)
            .then(response => {
              select(`#chart`)
                    .datum(response.data)
                    .call(chart);
            }).catch(e => {
                // FIXME Proper error handling
              console.log(e);
            });
  }
};
</script>
