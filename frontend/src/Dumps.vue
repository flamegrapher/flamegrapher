<template>
    <div>
        <b-row align-h="center" align-v="center">
            <flower-spinner :animation-duration="2500" :size="100" color="#b95e42" v-show="loading"/>
        </b-row>
        <back></back>
    </div>
</template>
<script>
export default {
  data () {
    return {
      loading: true
    };
  }
};
</script>
