import Vue from 'vue';
import VueRouter from 'vue-router';
import BootstrapVue from "bootstrap-vue";
import App from './App.vue';
import Home from './Home.vue';
import Dumps from './Dumps.vue';
import Flames from './Flames.vue';
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-vue/dist/bootstrap-vue.css";

Vue.use(BootstrapVue);
Vue.use(VueRouter);

const routes = [
  { path: '/', component: Home },
  { path: '/dumps', component: Dumps },
  { path: '/flames/:pid/:recordingId', component: Flames, props: true }
];

const router = new VueRouter({
  routes: routes
});

Vue.component('back', {
  template:'<b-row class="text-left"><b-col><a :href="`#/`">Back to home</a></b-col></b-row>'
});

new Vue({
  el: '#app',
  router,
  render: h => h(App)
});
