export default {
  methods: {
    save: function (item) {
      this.$set(item, "loading", true);
      this.$http
                .post("/api/save/" + item.pid + "/" + item.recording)
                .then(response => {
                  item.loading = false;
                  this.$notify({
                    title: "Success",
                    text: "Dump saved to S3 storage: " + response.data.key,
                    type: "success"
                  });
                })
                .catch(e => {
                  this.notifyError(e);
                  item.loading = false;
                });
    },
    notifyError: function (error) {
      this.$notify({
        title: error.message,
        text: error.response.data,
        type: "error",
        duration: 10000
      });
    }
  }
};
