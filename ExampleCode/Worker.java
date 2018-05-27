public class Worker {


    private Thread thread;
    private FinishJobListener listener;


    public Worker(FinishJobListener listener) {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 5; i++) {
                    try {
                        listener.report(i);
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                listener.finish();

            }
        });
    }

    public void startWork() {
        thread.start();
    }

    interface FinishJobListener {

        void report(int i);

        void finish();

    }

    //Example Code:

//    Worker worker = new Worker(new Worker.FinishJobListener() {
//        @Override
//        public void report(int i) {
//            System.out.println(i);
//        }
//
//        @Override
//        public void finish() {
//            System.out.println("finish job");
//        }
//    });
//
//        worker.startWork();
//
//}
}
