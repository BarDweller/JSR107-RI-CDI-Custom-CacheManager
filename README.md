JCache Annotations for CDI in Bluemix.
--------------------------------------

This is a simple adapter layer to allow JSR107 Annotations to be used with
Redisson's JCache API (Could easily be adapted to others), configured automatically 
from the `VCAP_SEERVICS` environment var when hosted within Bluemix.

The code is pretty much entirely based from the Annotations & CDI layer from 
the JCache RI, at https://github.com/jsr107/RI
