package com.liyunx.groot.processor

import com.liyunx.groot.GrootTestNGTestCase
import org.testng.annotations.Test

import static com.liyunx.groot.DefaultVirtualRunner.noopWith
import static com.liyunx.groot.DefaultVirtualRunner.sv

class HooksProcessorGroovyTest extends GrootTestNGTestCase {

    @Test
    public void testHooksProcessor() {
        sv("x", 0)
        sv("y", 0)
        noopWith("前置处理器标准写法测试") {
            setupBefore {
                hooks {
                    hook("\${vars.put('x', x + 1)}")
                    hook("\${vars.put('y', y + 1)}")
                }
            }
            setupAfter {
                hooks {
                    hook("\${vars.put('x', x + 1)}")
                    hook("\${vars.put('y', y + 1)}")
                }
            }
            validate {
                equalTo("\${(x + y)?int}", 4)
            }
        }
    }

    @Test
    public void testHooksProcessor2() {
        sv("x", 0)
        sv("y", 0)
        noopWith("前置处理器合并写法测试") {
            setup {
                before {
                    hooks {
                        hook("\${vars.put('x', x + 1)}")
                        hook("\${vars.put('y', y + 1)}")
                    }
                }
                after {
                    hooks {
                        hook("\${vars.put('x', x + 1)}")
                        hook("\${vars.put('y', y + 1)}")
                    }
                }
            }
            validate {
                equalTo("\${(x + y)?int}", 4)
            }
        }
    }

    @Test
    public void testHooksProcessor3() {
        sv("x", 0)
        sv("y", 0)
        noopWith("前置处理器合并写法测试") {
            setup {
                hooks {
                    hook("\${vars.put('x', x + 1)}")
                    hook("\${vars.put('y', y + 1)}")
                }
            } {
                hooks {
                    hook("\${vars.put('x', x + 1)}")
                    hook("\${vars.put('y', y + 1)}")
                }
            }
            validate {
                equalTo("\${(x + y)?int}", 4)
            }
        }
    }

}
