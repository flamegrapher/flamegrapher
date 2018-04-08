<template>
    <div>
        <div id="chart"></div>
        <back></back>
    </div>
</template>
<script>
// import axios from "axios";
// import d3 from "d3-flame-graph";
// export default {
//   mounted() {
//     var flameGraph = d3.flameGraph()
//                         .width(960)
//                         .cellHeight(18)
//                         .transitionDuration(750)
//                         .minFrameSize(5)
//                         .transitionEase(d3.easeCubic)
//                         .sort(true)
//                         //Example to sort in reverse order
//                         //.sort(function(a,b){ return d3.descending(a.name, b.name);})
//                         .title("")
//                         .onClick(onClick);
//     const url = "/flame/api/flames/" + pid + "/" + recordingId;
//     d3.json(url, function(error, data) {
//       // FIXME: Handle error properly
//       if (error) return console.warn(error);
//       d3.select("#chart")
//           .datum(data)
//           .call(flameGraph);
//     });
//   }
// }
</script>
