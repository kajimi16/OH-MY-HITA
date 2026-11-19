package com.stupidtree.hitax.agent.tools

class GetTimetableTool : SuspendingReActTool {
    override suspend fun execute(input: ReActToolInput): String = ToolHelper.timetableText(input)
}
