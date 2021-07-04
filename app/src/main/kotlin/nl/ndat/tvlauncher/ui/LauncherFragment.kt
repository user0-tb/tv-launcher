package nl.ndat.tvlauncher.ui

import android.Manifest
import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.ndat.tvlauncher.R
import nl.ndat.tvlauncher.data.entity.CollectionTile
import nl.ndat.tvlauncher.data.entity.Tile
import nl.ndat.tvlauncher.data.repository.TileRepository
import nl.ndat.tvlauncher.databinding.FragmentLauncherBinding
import nl.ndat.tvlauncher.ui.adapter.TileListAdapter
import nl.ndat.tvlauncher.util.getIntent
import org.koin.android.ext.android.inject

class LauncherFragment : Fragment() {
	private var _binding: FragmentLauncherBinding? = null
	private val binding get() = _binding!!

	private val backgroundContract = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
		binding.container.background = WallpaperManager.getInstance(requireContext()).drawable
	}

	private val tileRepository: TileRepository by inject()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		_binding = FragmentLauncherBinding.inflate(inflater, container, false)
		backgroundContract.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

		binding.settings.setOnFocusChangeListener { _, hasFocus ->
			val color = ContextCompat.getColor(requireContext(), if (hasFocus) R.color.lb_tv_white else R.color.lb_grey)
			val animator = ValueAnimator.ofArgb(binding.settings.imageTintList!!.defaultColor, color)
			animator.addUpdateListener {
				binding.settings.imageTintList = ColorStateList.valueOf(it.animatedValue as Int)
			}
			animator.duration = resources.getInteger(R.integer.button_animation_duration).toLong()
			animator.start()
		}

		binding.settings.setOnClickListener {
			startActivity(
				Intent(Settings.ACTION_SETTINGS),
				ActivityOptionsCompat.makeScaleUpAnimation(
					binding.settings,
					0,
					0,
					binding.settings.width,
					binding.settings.height
				).toBundle()
			)
		}

		val tileAdapter = TileListAdapter(requireContext()).apply {
			onActivate = { tile: Tile, view: View ->
				if (tile.uri != null) startActivity(
					tile.getIntent(),
					ActivityOptionsCompat.makeScaleUpAnimation(
						view,
						0,
						0,
						view.width,
						view.height
					).toBundle()
				)
			}

			onMenu = { tile: Tile, view: View ->
				// FIXME: Add more fancy menu design
				PopupMenu(requireContext(), view, Gravity.BOTTOM).apply {
					menu.add(0, 2, 2, R.string.move_left)
					menu.add(0, 3, 3, R.string.move_right)
					setOnMenuItemClickListener { item ->
						lifecycleScope.launch {
							when (item.itemId) {
								2 -> tileRepository.moveCollectionTile(CollectionTile.CollectionType.HOME, tile, -1)
								3 -> tileRepository.moveCollectionTile(CollectionTile.CollectionType.HOME, tile, 1)
							}
						}

						true
					}
				}.show()
			}
		}

		tileRepository.getHomeTiles().observe(viewLifecycleOwner) { tiles ->
			tileAdapter.items = tiles
		}

		binding.tiles.adapter = tileAdapter
		binding.tiles.requestFocus()

		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}
