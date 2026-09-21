package com.example.mobile_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobile_app.data.model.address.AddressResponse
import com.example.mobile_app.data.model.address.CreateAddressRequest
import com.example.mobile_app.data.model.address.GeoPlace
import com.example.mobile_app.data.model.address.UpdateAddressRequest
import com.example.mobile_app.data.remote.RetrofitClient
import com.example.mobile_app.data.repository.AddressRepository
import com.example.mobile_app.ui.state.UiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class AddressViewModel(private val repository: AddressRepository) : ViewModel() {

    private val _addresses = MutableStateFlow<UiState<List<AddressResponse>>>(UiState.Loading)
    val addresses: StateFlow<UiState<List<AddressResponse>>> = _addresses.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Manzil xizmat hududidami: addressId → true/false (null = tekshirilmagan). */
    private val _serviceMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val serviceMap: StateFlow<Map<String, Boolean>> = _serviceMap.asStateFlow()

    // ── Manzil dialogi holati ──────────────────────────────────────────
    // ViewModel backstack entry'da yashagani uchun "Xaritadan tanlash" orqali
    // MapPicker'ga o'tib qaytganda ham dialog YOPILMAYDI va yozilganlar o'chmaydi.
    private val _dialogOpen = MutableStateFlow(false)
    val dialogOpen: StateFlow<Boolean> = _dialogOpen.asStateFlow()

    private val _editingId = MutableStateFlow<String?>(null)
    val editingId: StateFlow<String?> = _editingId.asStateFlow()

    // Forma maydonlari (dialog ichidagi remember o'rniga — omon qoladi)
    private val _formLabel = MutableStateFlow("")
    val formLabel: StateFlow<String> = _formLabel.asStateFlow()

    private val _formLine2 = MutableStateFlow("")
    val formLine2: StateFlow<String> = _formLine2.asStateFlow()

    private val _formIsDefault = MutableStateFlow(false)
    val formIsDefault: StateFlow<Boolean> = _formIsDefault.asStateFlow()

    private val _formPicked = MutableStateFlow<GeoPlace?>(null)
    val formPicked: StateFlow<GeoPlace?> = _formPicked.asStateFlow()

    fun openAddDialog() {
        resetForm()
        _editingId.value = null
        _dialogOpen.value = true
    }

    fun openEditDialog(a: AddressResponse) {
        _formLabel.value = a.label ?: ""
        _formLine2.value = a.line2 ?: ""
        _formIsDefault.value = a.defaultAddress == true
        _formPicked.value =
            if (a.name != null && a.latitude != null && a.longitude != null) {
                GeoPlace(a.name, a.latitude, a.longitude)
            } else null
        _editingId.value = a.id
        _dialogOpen.value = true
    }

    fun closeDialog() {
        _dialogOpen.value = false
        _editingId.value = null
        resetForm()
    }

    fun setFormLabel(v: String) { _formLabel.value = v }
    fun setFormLine2(v: String) { _formLine2.value = v }
    fun setFormIsDefault(v: Boolean) { _formIsDefault.value = v }

    /** Xarita sahifasidan qaytgan nuqta — dialog ochiq qoladi, forma to'ladi. */
    fun onMapPicked(place: GeoPlace) {
        _formPicked.value = place
        if (_formLabel.value.isBlank()) {
            _formLabel.value = place.name.substringBefore(",").trim()
        }
    }

    /** Saqlash: picked shart; tahrir bo'lsa update, aks holda create. */
    fun submitDialog() {
        val p = _formPicked.value ?: return
        val request = CreateAddressRequest(
            label = _formLabel.value.trim().ifBlank { null },
            name = p.name,
            latitude = p.lat,
            longitude = p.lng,
            line2 = _formLine2.value.trim().ifBlank { null },
            phone = null,
            defaultAddress = _formIsDefault.value
        )
        val id = _editingId.value
        if (id == null) addAddress(request) else updateAddress(id, request)
        closeDialog()
    }

    private fun resetForm() {
        _formLabel.value = ""
        _formLine2.value = ""
        _formIsDefault.value = false
        _formPicked.value = null
    }

    // Geo qidiruv holati (manzil dialogi uchun)
    private val _searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<GeoPlace>>(emptyList())
    val searchResults: StateFlow<List<GeoPlace>> = _searchResults.asStateFlow()
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    init {
        load()
        observeLanguage()
        observeSearch()
    }

    private fun observeLanguage() {
        viewModelScope.launch {
            com.example.mobile_app.util.AppLanguage.flow.drop(1).collect {
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _addresses.value = UiState.Loading
            repository.listAddresses().collect { result ->
                result.fold(
                    onSuccess = {
                        _addresses.value = UiState.Success(it)
                        refreshServiceability(it)
                    },
                    onFailure = { _addresses.value = UiState.Error(it.message ?: "Xatolik") }
                )
            }
        }
    }

    /** Har bir koordinatali manzil xizmat hududidami — kartalarda ko'rsatiladi. */
    private fun refreshServiceability(list: List<AddressResponse>) {
        viewModelScope.launch {
            val map = mutableMapOf<String, Boolean>()
            for (a in list) {
                val lat = a.latitude
                val lng = a.longitude
                if (lat != null && lng != null) {
                    repository.checkCoverage(lat, lng).onSuccess { r ->
                        map[a.id] = r.served
                    }
                }
            }
            _serviceMap.value = map
        }
    }

    /** Bitta nuqta tekshiruvi (checkout uchun). */
    suspend fun checkPoint(lat: Double, lng: Double): Boolean {
        return repository.checkCoverage(lat, lng).getOrNull()?.served == true
    }

    fun addAddress(request: CreateAddressRequest) {
        viewModelScope.launch {
            _saving.value = true
            repository.createAddress(request).fold(
                onSuccess = { load() },
                onFailure = { _message.value = it.message ?: "Xatolik" }
            )
            _saving.value = false
        }
    }

    fun updateAddress(id: String, request: CreateAddressRequest) {
        viewModelScope.launch {
            _saving.value = true
            val update = UpdateAddressRequest(
                label = request.label,
                name = request.name,
                latitude = request.latitude,
                longitude = request.longitude,
                line2 = request.line2,
                phone = request.phone,
                defaultAddress = request.defaultAddress
            )
            repository.updateAddress(id, update).fold(
                onSuccess = { load() },
                onFailure = { _message.value = it.message ?: "Xatolik" }
            )
            _saving.value = false
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            repository.deleteAddress(id).fold(
                onSuccess = { load() },
                onFailure = { _message.value = it.message ?: "Xatolik" }
            )
        }
    }

    fun setDefault(id: String) {
        viewModelScope.launch {
            repository.setDefaultAddress(id).fold(
                onSuccess = { load() },
                onFailure = { _message.value = it.message ?: "Xatolik" }
            )
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    /** Qidiruv matni (debounce bilan nomzodlar yuklanadi) */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.trim().length < 3) {
                        _searchResults.value = emptyList()
                        _searching.value = false
                        return@collect
                    }
                    _searching.value = true
                    repository.searchPlaces(query.trim()).fold(
                        onSuccess = { _searchResults.value = it },
                        onFailure = { _searchResults.value = emptyList() }
                    )
                    _searching.value = false
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AddressViewModel(AddressRepository(RetrofitClient.addressApiService)) }
        }
    }
}