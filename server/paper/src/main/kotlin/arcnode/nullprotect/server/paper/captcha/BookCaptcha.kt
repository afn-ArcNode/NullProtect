/*
 *    Copyright 2024 ArcNode (AFterNode)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package arcnode.nullprotect.server.paper.captcha

import arcnode.nullprotect.server.paper.plugin
import cn.afternode.commons.bukkit.kotlin.book
import cn.afternode.commons.bukkit.kotlin.sub
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.entity.Player
import java.awt.Color
import java.util.concurrent.ThreadLocalRandom

private val colorPool = mapOf(
    "green" to Color.green,
    "orange" to Color.orange,
    "black" to Color.black,
    "blue" to Color.blue,
    "gray" to Color.gray,
    "red" to Color.red,
    "pink" to Color.pink
)

class BookCaptcha(override val player: Player): ICaptcha {
    init {
        val targetColor = colorPool.keys.random()
        val text = "CLICK ${targetColor.uppercase()} TO COMPLETE"

        player.openBook(book {
            title {
                text("CAPTCHA")
            }
            page {
                for (entry in colorPool) {
                    sub {
                        text(text, entry.value)
                        if (entry.key == targetColor) {
                            click(ClickEvent.callback {
                                plugin.captcha.accept(player)
                            })
                        } else {
                            click(ClickEvent.callback {
                                plugin.captcha.fail(player)
                            })
                        }
                    }
                    line()
                }
            }
        })
    }
}