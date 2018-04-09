<template>
    <div>
        <div id="chart"></div>
        <back></back>
    </div>
</template>
<script>
import axios from "axios";
import { flamegraph } from "d3-flame-graph";
import "d3-flame-graph/dist/d3-flamegraph.css";
export default {
  mounted () {
    const chart = flamegraph()
        .width(960)
        .cellHeight(18)
        .transitionDuration(750)
        .sort(true)
        .title("");
    const url = "/flame/api/flames/" + this.$route.params.pid + "/" + this.$route.params.recordingId;
    axios.get(url)
         .then(response => {
           flamegraph.select(`#chart`)
                       .datum(response.data)
                       .call(chart);
         }).catch(e => {
             // FIXME Proper error handling
           console.log(e);
         });
  }
};
</script>
