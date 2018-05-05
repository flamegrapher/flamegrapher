export default {
  methods: {
    save: function (item, api, message) {
      this.$set(item, "loading", true);
      this.$http
                .post(api + item.pid + "/" + item.recording)
                .then(response => {
                  item.loading = false;
                  this.$notify({
                    title: "Success",
                    text: message + response.data.key,
                    type: "success"
                  });
                })
                .catch(e => {
                  this.notifyError(e);
                  item.loading = false;
                });
    },
    saveFlames: function (item) {
      this.save(item, "/api/save/flame/", "Flames saved to S3 storage: ");
    },
    saveDump: function (item) {
      this.save(item, "/api/save/", "Dump saved to S3 storage: ");
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
