<template>
    <div>
        <b-row align-h="center" align-v="center">
            <flower-spinner :animation-duration="2500" :size="100" color="#b95e42" v-show="loading"/>
        </b-row>
        <b-row>
          <b-col>
              <b-table striped hover fixed :items="items" :fields="fields">
              <template slot="actions" slot-scope="row">
                <b-btn size="sm" :href="`${row.item.url}`">Download</b-btn>
                <b-btn size="sm" :href="`#/flames/${row.item.pid}/${row.item.recording}`">View flames</b-btn>
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
  name: "saves",
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
    const url = "/api/saves/";
    this.$http
      .get(url)
      .then(response => {
        response.data.forEach(element => {
          const tokens = element.key.split(".");
          var item = {};
          item.pid = tokens[0];
          item.recording = tokens[1];
          item.url = element.url;
          this.items.push(item);
        });
        this.loading = false;
      })
      .catch(e => {
        this.notifyError(e);
      });
  }
};
</script>
