# System integration research

Stock Android applications cannot register another phone's heap as physical RAM. The v0.1 service is usable only by software that explicitly speaks the RAMBridge protocol.

System-wide paging would require kernel and OS integration, careful page-fault handling, memory reclaim integration, and reliable network behavior. A remote network link has much higher latency than local DRAM, so it may perform poorly for general-purpose paging. Investigate feasibility separately; do not promise system-wide RAM expansion.
