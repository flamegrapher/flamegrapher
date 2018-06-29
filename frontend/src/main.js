import Vue from "vue";
import VueRouter from "vue-router";
import BootstrapVue from "bootstrap-vue";
import App from "./App.vue";
import Home from "./Home.vue";
import Dumps from "./Dumps.vue";
import Flames from "./Flames.vue";
import Saves from "./Saves.vue";
import Flamegraph from "./Flamegraph.vue";
import Upload from "./Upload.vue";
import { FlowerSpinner } from "epic-spinners";
import Notifications from "vue-notification";
import axios from "axios";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-vue/dist/bootstrap-vue.css";

Vue.use(BootstrapVue);
Vue.use(VueRouter);
Vue.use(Notifications);
Vue.prototype.$http = axios;

const routes = [
  { path: "/", component: Home },
  { path: "/dumps", component: Dumps },
  { path: "/flames/:eventType/:pid/:recording", component: Flames },
  { path: "/saves", component: Saves },
  { path: "/upload", component: Upload}
];

const router = new VueRouter({
  routes: routes
});

Vue.component("back", {
  template: '<b-row class="text-left"><b-col><a :href="`#/`">Back to home</a></b-col></b-row>'
});

Vue.component("flower-spinner", FlowerSpinner);
Vue.component("flamegraph-dropdown", Flamegraph);

new Vue({
  el: "#app",
  router,
  render: h => h(App)
});

// Configure axios error handling using interceptors
axios.interceptors.response.use(function (response) {
  return response;
}, function (error) {
  console.log(error);
  return Promise.reject(error); // this is the important part
});

