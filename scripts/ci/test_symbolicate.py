#!/usr/bin/env python3
"""Tests for symbolicate.py: frame parsing, Breakpad ids, the request and the rendered stack."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import symbolicate  # noqa: E402

LOGCAT = """\
10-06 10:40:37.046  8398  8398 F DEBUG   :       #00 pc 000000000495dd78  /data/app/~~x==/io.github.mikespl187.vola-y==/base.apk!libxul.so (offset 0x1000) (BuildId: b53c7084577cbba340dc13a32d813ab69c72e9df)
10-06 10:40:37.046  8398  8398 F DEBUG   :       #01 pc 000000000759567d  /data/app/~~x==/base.apk!libxul.so (BuildId: b53c7084577cbba340dc13a32d813ab69c72e9df)
10-06 10:40:37.046  8398  8398 F DEBUG   :       #02 pc 0000000000062c3b  /apex/com.android.runtime/lib64/bionic/libc.so (__pthread_start+59) (BuildId: 2a0b3d8e1e5b7c9f00112233445566778899aabb)
"""
JOURNAL = """\
Process main stopped: native crash (signal 11)
Backtrace (thread Gecko):
#00 pc 0495dd78  libxul.so (BuildId: b53c7084577cbba340dc13a32d813ab69c72e9df)
#01 pc 0759567d  libxul.so (BuildId: b53c7084577cbba340dc13a32d813ab69c72e9df)
"""


class SymbolicateTest(unittest.TestCase):
    def test_debug_id_is_the_build_id_in_guid_order(self):
        self.assertEqual(
            symbolicate.debug_id("b53c7084577cbba340dc13a32d813ab69c72e9df"),
            "84703CB57C57A3BB40DC13A32D813AB60",
        )

    def test_logcat_and_journal_frames_are_read(self):
        logcat = symbolicate.backtraces(LOGCAT)
        self.assertEqual(len(logcat), 1)
        self.assertEqual([frame[2] for frame in logcat[0]], ["libxul.so", "libxul.so", "libc.so"])
        self.assertEqual(logcat[0][0][1], 0x495DD78)
        journal = symbolicate.backtraces(JOURNAL)
        self.assertEqual([frame[0] for frame in journal[0]], [0, 1])

    def test_a_new_frame_zero_starts_a_new_backtrace(self):
        self.assertEqual(len(symbolicate.backtraces(LOGCAT + LOGCAT)), 2)

    def test_request_lists_each_module_once(self):
        body = symbolicate.request_body(symbolicate.backtraces(LOGCAT)[0])
        job = body["jobs"][0]
        self.assertEqual(job["memoryMap"][0], ["libxul.so", "84703CB57C57A3BB40DC13A32D813AB60"])
        self.assertEqual(len(job["memoryMap"]), 2)
        self.assertEqual(job["stacks"][0][:2], [[0, 0x495DD78], [0, 0x759567D]])

    def test_named_frames_and_unknown_libraries_render(self):
        result = {"results": [{"stacks": [[
            {"function": "mozilla::AndroidBridge::GetStaticMethodID", "function_offset": "0x38",
             "file": "widget/android/AndroidBridge.cpp", "line": 120},
            {"function": "mozilla::dom::WebAuthnTransactionParent::RecvRequestIsUVPAA",
             "function_offset": "0xf4"},
            {"module_offset": "0x62c3b"},
        ]]}]}
        blocks = symbolicate.symbolicate(LOGCAT, fetch=lambda body: result)
        self.assertEqual(blocks[0][0], "#00 libxul.so mozilla::AndroidBridge::GetStaticMethodID+0x38 "
                                       "widget/android/AndroidBridge.cpp:120")
        self.assertIn("RecvRequestIsUVPAA+0xf4", blocks[0][1])
        self.assertEqual(blocks[0][2], "#02 libc.so pc 00062c3b")

    def test_a_server_failure_keeps_the_raw_frames(self):
        def fail(body):
            raise OSError("403")
        blocks = symbolicate.symbolicate(JOURNAL, fetch=fail)
        self.assertEqual(blocks[0][0], "symbolication failed: 403")
        self.assertEqual(blocks[0][1], "#00 libxul.so pc 0495dd78")

    def test_text_without_build_ids_has_no_backtrace(self):
        self.assertEqual(symbolicate.symbolicate("java.lang.IllegalStateException\n\tat a.b(c.kt:1)"), [])


if __name__ == "__main__":
    unittest.main()
