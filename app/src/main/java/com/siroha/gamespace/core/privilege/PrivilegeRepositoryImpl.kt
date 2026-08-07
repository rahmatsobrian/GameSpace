package com.siroha.gamespace.core.privilege

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivilegeRepositoryImpl @Inject constructor(
    private val rootSource: RootPrivilegeSource,
    private val shizukuSource: ShizukuPrivilegeSource
) : PrivilegeRepository {

    private val _state = MutableStateFlow(PrivilegeState())
    override val state: StateFlow<PrivilegeState> = _state.asStateFlow()

    override fun refresh() {
        _state.update {
            it.copy(root = rootSource.peekStatus(), shizuku = shizukuSource.peekStatus())
        }
    }

    override suspend fun requestRoot() {
        _state.update { it.copy(root = it.root.copy(state = AccessState.CHECKING)) }
        val result = rootSource.requestAccess()
        _state.update { it.copy(root = result) }
    }

    override suspend fun requestShizuku() {
        _state.update { it.copy(shizuku = it.shizuku.copy(state = AccessState.CHECKING)) }
        val result = shizukuSource.requestAccess()
        _state.update { it.copy(shizuku = result) }
    }

    override suspend fun execPrivileged(command: String): PrivilegedExecResult {
        return when (_state.value.activeTier) {
            PrivilegeTier.ROOT -> rootSource.exec(command)
            PrivilegeTier.SHIZUKU -> shizukuSource.exec(command)
            PrivilegeTier.PUBLIC_ONLY -> PrivilegedExecResult.Unavailable
        }
    }
}
