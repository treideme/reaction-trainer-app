//
//
//public class PodsManager {
//    private MainActivity parent;
//    private Context context;
//    private BluetoothAdapter bluetoothAdapter;
//    private BluetoothLeScanner bluetoothLeScanner;
//
//    private Map<String, Pod> knownDevices = new HashMap<String, Pod>();
//
//    private boolean hasPermission = false;
//    private boolean scanStarted = false;
//
//    // Device scan callback.
//    @SuppressLint("MissingPermission")
//    private ScanCallback scanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            BluetoothDevice device = result.getDevice();
//            String address = device.getAddress();
//            String name = device.getName();
//            if (name != null && name.startsWith("Reaction Trainer")) {
//                if (knownDevices.containsKey(address)) {
//                    // FIXME: Update
//                } else {
//                    Pod p = new Pod(device, context);
//                    knownDevices.put(address, p);
//                    Log.d("PodsManager", "added (" + address + ", " + name + ") = " + result.toString());
//                }
//            }
//        }
//    };
//
//    public PodsManager(MainActivity owner, Context context) {
//        this.parent = owner;
//        this.context = context;
//        Log.d("PodsManager", "Created");
//    }
//
//    public void onHasPermission() {
//        hasPermission = true;
//        onStart();
//        Log.d("PodsManager", "onHasPermission");
//    }
//
//    public void onStart() {
//        if(!scanStarted && hasPermission) {
//            findPods();
//        }
//        Log.d("PodsManager", "onStart");
//    }
//
//    @SuppressLint("MissingPermission")
//    public void onStop() {
//        if(scanStarted) {
//            bluetoothLeScanner.stopScan(scanCallback);
//            scanStarted = false;
//        }
//        Log.d("PodsManager", "onStop");
//    }
//
//    @SuppressLint("MissingPermission")
//    private void findPods() {
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if(!bluetoothAdapter.isEnabled()) {
//            bluetoothAdapter.enable();
//        }
//        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
//        bluetoothLeScanner.startScan(scanCallback);
//        scanStarted = true;
//        Log.d("PodsManager", "findPods");
//    }
//}
