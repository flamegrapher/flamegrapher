<template>
    <b-dropdown size="sm" text="Flamegraph" :disabled="disabled">
    <b-dropdown-item :href="`#/flames/cpu/${item.pid}/${item.recording}`">CPU</b-dropdown-item>
    <b-dropdown-item :href="`#/flames/locks/${item.pid}/${item.recording}`">Locks</b-dropdown-item>
    <b-dropdown-item :href="`#/flames/exceptions/${item.pid}/${item.recording}`">Exceptions</b-dropdown-item>
    <b-dropdown-item :href="`#/flames/alloc-tlab/${item.pid}/${item.recording}`">Allocations in new TLAB</b-dropdown-item>
    <b-dropdown-item :href="`#/flames/alloc-out-tlab/${item.pid}/${item.recording}`"> Allocations outside TLAB</b-dropdown-item>
    </b-dropdown>
</template>
<script>
export default {
  name: "flamegraph-dropdown",
  props: ["item", "disabled"]
};
</script>
