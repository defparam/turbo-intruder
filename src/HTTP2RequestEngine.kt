package burp
import burp.Request
import burp.RequestEngine
import burp.Utils
import java.lang.Exception
import java.net.URL
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

open class HTTP2RequestEngine(url: String, val threads: Int, maxQueueSize: Int, var requestsPerConnection: Int, override val maxRetriesPerRequest: Int, override val callback: (Request, Boolean) -> Boolean, override var readCallback: ((String) -> Boolean)?): RequestEngine() {

    private val connectionPool = ArrayList<Connection>(threads)

    init {
        requestQueue = if (maxQueueSize > 0) {
            LinkedBlockingQueue(maxQueueSize)
        }
        else {
            LinkedBlockingQueue()
        }

        target = URL(url)
        completedLatch = CountDownLatch(threads)

        for (j in 1..threads) {
            connections.incrementAndGet()
            connectionPool.add(Connection(target, LinkedBlockingQueue(1), requestQueue, requestsPerConnection, this))
        }

        thread(priority = 1) {
            manageConnections()
        }
    }

    // just handles dead connections
    private fun manageConnections() {
        // showStats changes state from 1 to 2
        // then waits on the completedLatch to hit 3
        while (attackState.get() < 3) {
            for (i in 1..threads) {
                val con = connectionPool[i - 1]
                if (con.done) {
                    continue
                }


                // don't sit around waiting for recycling
                // todo re-enable and check effect on performance
//                if (con.state == Connection.HALFCLOSED) {
//                    connections[i - 1] = Connection(target, responseReadCount, requestQueue, requestsPerConnection)
//                    continue
//                }

                if (con.state == Connection.CLOSED) {
                    if (con.hasInflightRequests() || con.seedQueue.size > 0 || attackState.get() < 3) {
                        val inflight = con.getInflightRequests()
                        val seedQueue = LinkedBlockingQueue(inflight)
                        seedQueue.addAll(con.seedQueue)
                        if (con.hasInflightRequests()) {
                            retries.getAndIncrement()
                            Utils.out("Connection died, re-queueing "+inflight.size+" unanswered requests.")
                        }
                        connections.incrementAndGet()
                        connectionPool[i - 1] = Connection(target, seedQueue, requestQueue, requestsPerConnection, this)
                    }
                }
            }
            Thread.sleep(10)
        }
    }

    override fun start(timeout: Int) {
        attackState.set(1)
        start = System.nanoTime()
    }

    override fun buildRequest(template: String, payloads: List<String?>, learnBoring: Int?, label: String?): Request {
        return Request(template, payloads, learnBoring?: 0, label)
    }


//    fun queue(request: ByteArray) {
//        if (fullyQueued) {
//            throw IllegalStateException("Cannot queue any more items - the attack has finished")
//        }
//        val queued = requestQueue.offer(request, 1, TimeUnit.SECONDS)
//        if (!queued) {
//            throw IllegalStateException("Timeout queuing request")
//        }
//    }
}
