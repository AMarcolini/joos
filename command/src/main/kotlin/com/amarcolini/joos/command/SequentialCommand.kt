package com.amarcolini.joos.command

/**
 * A command that runs commands in sequence.
 */
class SequentialCommand(
    isInterruptable: Boolean = true,
    vararg commands: Command
) : CommandGroup(false, commands, isInterruptable) {
    constructor(vararg commands: Command): this(!commands.any { !it.isInterruptable }, *commands)

    private val commands = commands.toMutableList()
    override fun add(command: Command) {
        commands += command
        isInterruptable = isInterruptable && command.isInterruptable
    }

    private var index = -1

    override fun init() {
        index = 0
        commands[index].init()
    }

    override fun execute() {
        if (index < 0 || index >= commands.size) return

        commands[index].execute()

        if (commands[index].isFinished()) {
            commands[index].end(false)
            index++
            if (index < commands.size) commands[index].init()
        }
    }

    override fun isFinished() = index >= commands.size

    override fun end(interrupted: Boolean) {
        if (index < 0) return
        if (interrupted) commands[index].end(true)
    }
}