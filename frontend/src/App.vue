<template>
  <div id="app">
    <b-container>
      <b-row class="text-left">
          <b-col><h2>Flamegrapher 🔥</h2></b-col>
      </b-row>
      <b-row>
        <b-col>
          <b-table striped hover fixed :items="items" :fields="fields">
            <template slot="recording" slot-scope="row">
              <a :href="`#flames/${row.item.recording}`" v-if="row.item.hasDump">
                {{row.item.recording}} (view)
              </a>
              <div v-else>
                {{row.item.recording}}
              </div>
            </template>
            <template slot="actions" slot-scope="row">
              <b-button-group size="sm">
                <b-btn :disabled="row.item.state === 'Recording' || row.item.state === 'Dumped'" @click="start(row.item)">Start</b-btn>
                <b-btn :disabled="row.item.state === 'Not recording'" @click="stop(row.item)">Stop</b-btn>
                <b-btn :disabled="row.item.state === 'Not recording'" @click="dump(row.item)">Dump</b-btn>
              </b-button-group>
            </template>
          </b-table>
        </b-col>
      </b-row>
      <b-row class="text-left">
        <b-col>
          <a :href="`#`">
            View all available dumps  
          </a>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "app",
  data() {
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
        "actions"
      ],
      items: []
    };
  },
  loadList() {},
  // On load
  mounted() {
    this.$nextTick(function() {
      axios
        .get("/flame/api/list/")
        .then(response => {
          this.items = response.data;
        })
        .catch(e => {
          console.log(e);
        });
    });
  },
  methods: {
    start: function(item) {
      axios
        .get("/flame/api/start/" + item.pid)
        .then(response => {
          item.state = response.data.state;
          item.recording = response.data.recording;
        })
        .catch(e => {
          console.log(e);
        });
    },
    stop: function(item) {
      axios
        .get("/flame/api/stop/" + item.pid)
        .then(response => {
          item.state = response.data.state;
        })
        .catch(e => {
          console.log(e);
        });
    },
    dump: function(item) {
      axios
        .get("/flame/api/dump/" + item.pid + "/" + item.recording)
        .then(response => {
          item.state = response.data.state;
          item.hasDump = true;
        })
        .catch(e => {
          console.log(e);
        });
    }
  }
};
</script>

<style>
#app {
  font-family: "Avenir", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin-top: 40px;
}

h1,
h2 {
  font-weight: normal;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  display: inline-block;
  margin: 0 10px;
}

a,
h2 {
  color: #b95e42;
}
</style>
