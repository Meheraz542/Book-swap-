package com.aistudio.bookset.market.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.bookset.market.data.model.BookSet
import com.aistudio.bookset.market.data.repository.MarketplaceRepository
import com.aistudio.bookset.market.ui.components.NeumorphicButton
import com.aistudio.bookset.market.ui.components.NeumorphicCard
import com.aistudio.bookset.market.ui.theme.AccentCoral
import com.aistudio.bookset.market.ui.theme.AccentGreen
import com.aistudio.bookset.market.ui.theme.NeumorphicBackground
import com.aistudio.bookset.market.ui.theme.PrimaryBlue
import com.aistudio.bookset.market.ui.theme.TextMuted
import com.aistudio.bookset.market.ui.theme.TextPrimary
import com.aistudio.bookset.market.ui.theme.TextSecondary

@Composable
fun DealsScreen(
    repository: MarketplaceRepository,
    onBookSetClick: (BookSet) -> Unit
) {
    val deals by repository.megaDeals.collectAsState()
    val bookSets by repository.bookSets.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Flash deals",
                        tint = AccentCoral,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mega Deals & Bundles",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Exclusive limited-time discounts on full book sets",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        items(deals, key = { it.id }) { deal ->
            val linkedBookSet = bookSets.find { it.id == deal.bookSetId }
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentCoral)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = deal.discountBadge,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = AccentCoral,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ends in ${deal.endsInHours}h",
                                color = AccentCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = deal.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = deal.subtitle,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$${deal.dealPrice.toInt()}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$${deal.originalPrice.toInt()}",
                                    fontSize = 14.sp,
                                    color = TextMuted,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                            Text(
                                text = "Save $${(deal.originalPrice - deal.dealPrice).toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }

                        NeumorphicButton(
                            onClick = {
                                if (linkedBookSet != null) {
                                    onBookSetClick(linkedBookSet)
                                }
                            },
                            backgroundColor = PrimaryBlue,
                            modifier = Modifier
                                .height(44.dp)
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "View Bundle",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
