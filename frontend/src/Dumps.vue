<template>
    <div>
        <b-row align-h="center" align-v="center">
            <flower-spinner :animation-duration="2500" :size="100" color="#b95e42" v-show="loading"/>
        </b-row>
        <b-row>
          <b-col>
              <b-table striped hover fixed :items="items" :fields="fields">
              <template slot="actions" slot-scope="row">
                <b-btn size="sm" :href="`/api/dump/${row.item.pid}.${row.item.recording}.jfr`">Download</b-btn>
                <b-btn size="sm" :href="`#/flames/${row.item.pid}/${row.item.recording}`">View flames</b-btn>
              </template>
              </b-table>
          </b-col>
        </b-row>
        <back></back>
    </div>
</template>
<script>
export default {
  name: "dumps",
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
          label: "Recording #"
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
      .catch(error => {
        this.$notify({
          title: error.message,
          text: error.response.data,
          type: "error",
          duration: 10000
        });
      });
  }
};
</script>
